package org.analysis.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;

/** Viewer Swing — nœuds "Classe#méthode" (sans package) avec le label À L’INTÉRIEUR + DRAG des nœuds. */
public class CallGraphSwing extends JPanel {

    // caller(FQN#method) -> callees(FQN#method)
    private final Map<String, Set<String>> graph;

    // positions (centre des nœuds) dans le repère "monde" (avant zoom/pan)
    private final java.util.List<String> nodes = new ArrayList<>();
    private final Map<String, Point> pos = new LinkedHashMap<>();

    // rectangles calculés au dernier paint (dans le repère monde)
    private final Map<String, Rectangle> rects = new LinkedHashMap<>();

    // zoom/pan (transform écran -> monde : world = (screen - offset) / scale)
    private double scale = 1.0;
    private int offX = 0, offY = 0;
    private Point lastDragScreen;

    // drag de nœud
    private String draggingNode = null;
    private int grabDX = 0, grabDY = 0; // décalage (monde) entre souris et centre lors du press

    public CallGraphSwing(Map<String, Set<String>> graph) {
        this.graph = graph;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1600, 1000));
        buildLayout();
        enableInteractions();
    }

    /* ====================== interactions ====================== */
    private void enableInteractions() {
        // zoom (molette)
        addMouseWheelListener(e -> {
            double f = (e.getWheelRotation() < 0) ? 1.1 : (1/1.1);
            scale = Math.max(0.2, Math.min(6.0, scale * f));
            repaint();
        });

        // survol : curseur "main" quand on est sur un nœud
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                Point w = screenToWorld(e.getPoint());
                String hit = findNodeAt(w);
                setCursor(hit != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });

        // press : détecte si on attrape un nœud (sinon, pan)
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                lastDragScreen = e.getPoint();
                Point w = screenToWorld(e.getPoint());
                String hit = findNodeAt(w);
                if (hit != null) {
                    draggingNode = hit;
                    Rectangle r = rects.get(hit);
                    // centre du nœud (monde)
                    int cx = r.x + r.width / 2;
                    int cy = r.y + r.height / 2;
                    grabDX = w.x - cx;
                    grabDY = w.y - cy;
                } else {
                    draggingNode = null; // pan
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                draggingNode = null;
            }
        });

        // drag : si on a un nœud, on le déplace ; sinon on pan
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (draggingNode != null) {
                    // déplace le nœud dans le repère monde
                    Point w = screenToWorld(e.getPoint());
                    Rectangle r = rects.get(draggingNode);
                    if (r != null) {
                        int newCx = w.x - grabDX;
                        int newCy = w.y - grabDY;
                        pos.put(draggingNode, new Point(newCx, newCy)); // on stocke le centre
                        repaint();
                    }
                } else if (lastDragScreen != null) {
                    // pan classique (dans l'écran)
                    offX += e.getX() - lastDragScreen.x;
                    offY += e.getY() - lastDragScreen.y;
                    lastDragScreen = e.getPoint();
                    repaint();
                }
            }
        });
    }

    /* ====================== layout initial (cercle) ====================== */
    private void buildLayout() {
        Set<String> all = new LinkedHashSet<>(graph.keySet());
        graph.values().forEach(all::addAll);
        nodes.clear();
        nodes.addAll(all);

        int n = Math.max(1, nodes.size());
        int cx = 800, cy = 500, r = Math.min(cx, cy) - 140; // marge
        r = Math.max(r, 300);
        pos.clear();
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            pos.put(nodes.get(i), new Point(
                    cx + (int) (r * Math.cos(a)),
                    cy + (int) (r * Math.sin(a))
            ));
        }
    }

    /* ====================== utilitaires ====================== */
    private static String toClassHashMethod(String fqnHash) {
        if (fqnHash == null) return "";
        int sharp = fqnHash.indexOf('#');
        String cls = (sharp >= 0) ? fqnHash.substring(0, sharp) : fqnHash;
        String meth = (sharp >= 0) ? fqnHash.substring(sharp + 1) : "";
        int dot = cls.lastIndexOf('.');
        String simple = (dot >= 0) ? cls.substring(dot + 1) : cls;
        return (meth.isEmpty()) ? simple : (simple + "#" + meth);
    }

    private static Rectangle calcNodeRect(Graphics2D g2, Point center, String label) {
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(label);
        int h = fm.getAscent() + fm.getDescent();
        int padX = 14, padY = 8;
        int boxW = Math.max(60, w + padX * 2);
        int boxH = Math.max(28, h + padY * 2);
        return new Rectangle(center.x - boxW / 2, center.y - boxH / 2, boxW, boxH);
    }

    private Point screenToWorld(Point screen) {
        // inverse de la transform (translate + scale)
        int wx = (int) Math.round((screen.x - offX) / scale);
        int wy = (int) Math.round((screen.y - offY) / scale);
        return new Point(wx, wy);
    }

    private String findNodeAt(Point worldPoint) {
        for (var e : rects.entrySet()) {
            if (e.getValue().contains(worldPoint)) return e.getKey();
        }
        return null;
    }

    /* ====================== rendu ====================== */
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // transform
        g2.translate(offX, offY);
        g2.scale(scale, scale);

        // qualité
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // 1) calculer/rafraîchir les rectangles pour chaque nœud (repère monde)
        rects.clear();
        for (String n : nodes) {
            String label = toClassHashMethod(n);
            Rectangle r = calcNodeRect(g2, pos.get(n), label);
            rects.put(n, r);
        }

        // 2) tracer les arêtes (centre -> centre)
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(new Color(0x9AA5B1));
        for (var e : graph.entrySet()) {
            Rectangle rc = rects.get(e.getKey());
            if (rc == null) continue;
            int x1 = rc.x + rc.width / 2, y1 = rc.y + rc.height / 2;
            for (String v : e.getValue()) {
                Rectangle rt = rects.get(v);
                if (rt == null) continue;
                int x2 = rt.x + rt.width / 2, y2 = rt.y + rt.height / 2;
                g2.drawLine(x1, y1, x2, y2);
                drawArrowHead(g2, x1, y1, x2, y2);
            }
        }

        // 3) tracer les nœuds (fond + bord + label centré)
        for (String n : nodes) {
            Rectangle r = rects.get(n);
            String label = toClassHashMethod(n);

            // fond
            g2.setColor(Color.WHITE);
            RoundRectangle2D rr = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 18, 18);
            g2.fill(rr);

            // bord
            g2.setColor(new Color(0x2D6CDF));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(rr);

            // petit point à gauche
            int dotR = 5;
            g2.setColor(new Color(0x2D6CDF));
            g2.fillOval(r.x + 8 - dotR, r.y + r.height / 2 - dotR, dotR * 2, dotR * 2);

            // label centré
            g2.setColor(new Color(0x0F1C2E));
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(label);
            int textH = fm.getAscent();
            int tx = r.x + (r.width - textW) / 2;
            int ty = r.y + (r.height - fm.getHeight()) / 2 + textH - 1;
            g2.drawString(label, tx, ty);
        }

        g2.dispose();
    }

    private static void drawArrowHead(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double ang = Math.atan2(y2 - y1, x2 - x1);
        int len = 10;
        int xA = x2 - (int) (len * Math.cos(ang - Math.PI / 6));
        int yA = y2 - (int) (len * Math.sin(ang - Math.PI / 6));
        int xB = x2 - (int) (len * Math.cos(ang + Math.PI / 6));
        int yB = y2 - (int) (len * Math.sin(ang + Math.PI / 6));
        g2.drawLine(x2, y2, xA, yA);
        g2.drawLine(x2, y2, xB, yB);
    }

    /** Ouvre une fenêtre avec le graphe fourni. */
    public static void show(Map<String, Set<String>> subgraph) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Graphe d'appel (Classe#méthode)");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.getContentPane().add(new JScrollPane(new CallGraphSwing(subgraph)));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
