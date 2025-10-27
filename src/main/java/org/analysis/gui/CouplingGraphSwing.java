package org.analysis.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;

/**
 * Graphe de COUPLAGE pondéré entre classes.
 * - Nœuds = noms simples de classes (draggables)
 * - Arêtes orientées, épaisseur ∝ weight
 * - Label d’arête: "w=11  (0.478)"
 * - Zoom (molette), pan (drag sur fond), drag & drop des nœuds.
 */
public class CouplingGraphSwing extends JPanel {

    // A -> (B -> EdgeInfo)
    private final Map<String, Map<String, EdgeInfo>> graph;
    private final java.util.List<String> nodes = new ArrayList<>();
    private final Map<String, Point> pos = new LinkedHashMap<>();
    private final Map<String, Rectangle> rects = new LinkedHashMap<>();

    // interactions
    private double scale = 1.0;
    private int offX = 0, offY = 0;
    private Point lastDragScreen;
    private String draggingNode = null;
    private int grabDX = 0, grabDY = 0;

    /** Poids et ratio (w / total). */
    public static class EdgeInfo {
        public final int weight;
        public final double ratio;
        public EdgeInfo(int w, double r) { this.weight = w; this.ratio = r; }
    }

    public CouplingGraphSwing(Map<String, Map<String, EdgeInfo>> graph) {
        this.graph = graph;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1400, 900));
        buildLayout();
        enableInteractions();
    }

    public static void show(Map<String, Map<String, EdgeInfo>> graph) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Graphe de couplage (classes)");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.getContentPane().add(new JScrollPane(new CouplingGraphSwing(graph)));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    private void buildLayout() {
        Set<String> all = new LinkedHashSet<>(graph.keySet());
        for (var m : graph.values()) all.addAll(m.keySet());
        nodes.clear(); nodes.addAll(all);

        int n = Math.max(1, nodes.size());
        int cx = 700, cy = 450, r = Math.min(cx, cy) - 160;
        r = Math.max(r, 240);
        pos.clear();
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            pos.put(nodes.get(i), new Point(
                    cx + (int)(r * Math.cos(a)),
                    cy + (int)(r * Math.sin(a))
            ));
        }
    }

    private void enableInteractions() {
        addMouseWheelListener(e -> {
            double f = (e.getWheelRotation() < 0) ? 1.1 : (1/1.1);
            scale = Math.max(0.2, Math.min(5.0, scale * f));
            repaint();
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                setCursor(findNodeAt(screenToWorld(e.getPoint())) != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                lastDragScreen = e.getPoint();
                Point w = screenToWorld(e.getPoint());
                String hit = findNodeAt(w);
                if (hit != null) {
                    draggingNode = hit;
                    Rectangle r = rects.get(hit);
                    int cx = r.x + r.width/2, cy = r.y + r.height/2;
                    grabDX = w.x - cx; grabDY = w.y - cy;
                } else {
                    draggingNode = null;
                }
            }
            @Override public void mouseReleased(MouseEvent e) { draggingNode = null; }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (draggingNode != null) {
                    Point w = screenToWorld(e.getPoint());
                    Rectangle r = rects.get(draggingNode);
                    if (r != null) {
                        int newCx = w.x - grabDX;
                        int newCy = w.y - grabDY;
                        pos.put(draggingNode, new Point(newCx, newCy));
                        repaint();
                    }
                } else if (lastDragScreen != null) {
                    offX += e.getX() - lastDragScreen.x;
                    offY += e.getY() - lastDragScreen.y;
                    lastDragScreen = e.getPoint();
                    repaint();
                }
            }
        });
    }

    private Point screenToWorld(Point screen) {
        int wx = (int)Math.round((screen.x - offX) / scale);
        int wy = (int)Math.round((screen.y - offY) / scale);
        return new Point(wx, wy);
    }

    private String findNodeAt(Point world) {
        for (var e : rects.entrySet()) if (e.getValue().contains(world)) return e.getKey();
        return null;
    }

    private static Rectangle calcNodeRect(Graphics2D g2, Point center, String label) {
        FontMetrics fm = g2.getFontMetrics();
        int w = Math.max(80, fm.stringWidth(label) + 28);
        int h = 36;
        return new Rectangle(center.x - w/2, center.y - h/2, w, h);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(offX, offY);
        g2.scale(scale, scale);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        // rectangles des nœuds
        rects.clear();
        for (String n : nodes) rects.put(n, calcNodeRect(g2, pos.get(n), n));

        // épaisseur max pour la mise à l’échelle
        int maxW = 1;
        for (var m : this.graph.values())
            for (var ei : m.values()) maxW = Math.max(maxW, ei.weight);

        // arêtes
        for (var e1 : this.graph.entrySet()) {
            String a = e1.getKey();
            Rectangle ra = rects.get(a);
            int ax = ra.x + ra.width/2, ay = ra.y + ra.height/2;

            for (var e2 : e1.getValue().entrySet()) {
                String b = e2.getKey();
                Rectangle rb = rects.get(b);
                int bx = rb.x + rb.width/2, by = rb.y + rb.height/2;

                EdgeInfo info = e2.getValue();
                float thickness = (float)(1.0 + 4.0 * info.weight / (double)maxW);

                g2.setColor(new Color(0x9AA5B1));
                g2.setStroke(new BasicStroke(thickness));
                g2.drawLine(ax, ay, bx, by);
                drawArrowHead(g2, ax, ay, bx, by, thickness);

                String lbl = String.format(Locale.US, "w=%d  (%.3f)", info.weight, info.ratio);
                drawEdgeLabel(g2, lbl, ax, ay, bx, by);
            }
        }

        // nœuds
        for (String n : nodes) {
            Rectangle r = rects.get(n);
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 18, 18));
            g2.setColor(new Color(0x2D6CDF));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 18, 18));
            g2.fillOval(r.x + 8 - 5, r.y + r.height/2 - 5, 10, 10);

            g2.setColor(new Color(0x0F1C2E));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(n);
            int tx = r.x + (r.width - tw)/2;
            int ty = r.y + (r.height - fm.getHeight())/2 + fm.getAscent() - 1;
            g2.drawString(n, tx, ty);
        }

        g2.dispose();
    }

    private static void drawArrowHead(Graphics2D g2, int x1, int y1, int x2, int y2, float thickness) {
        double ang = Math.atan2(y2 - y1, x2 - x1);
        int len = 10 + Math.round(thickness);
        int xA = x2 - (int)(len * Math.cos(ang - Math.PI / 6));
        int yA = y2 - (int)(len * Math.sin(ang - Math.PI / 6));
        int xB = x2 - (int)(len * Math.cos(ang + Math.PI / 6));
        int yB = y2 - (int)(len * Math.sin(ang + Math.PI / 6));
        g2.drawLine(x2, y2, xA, yA);
        g2.drawLine(x2, y2, xB, yB);
    }

    private static void drawEdgeLabel(Graphics2D g2, String text, int x1, int y1, int x2, int y2) {
        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2;
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text), h = fm.getAscent();
        int pad = 3;
        g2.setColor(new Color(255,255,255,220));
        g2.fillRoundRect(mx - w/2 - pad, my - h - pad, w + 2*pad, h + 2*pad, 8, 8);
        g2.setColor(new Color(0x0F1C2E));
        g2.drawString(text, mx - w/2, my - 4);
    }
}
