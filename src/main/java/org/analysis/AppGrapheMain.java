package org.analysis;

import org.analysis.gui.AnalysisRunner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Graphe d'appel pour 4 classes (BoutiqueService, Produit, ProduitElectronique, Vendable).
 * - Drag & drop des nœuds (position figée)
 * - Pan (glisser sur le fond)
 * - Zoom molette, +/−, Adapter à la fenêtre, Réinitialiser layout
 */
public class AppGrapheMain {

    private static final Set<String> KEEP_CLASSES = Set.of(
            "org.example.codebase.BoutiqueService",
            "org.example.codebase.Produit",
            "org.example.codebase.ProduitElectronique",
            "org.example.codebase.Vendable"
    );

    public static void main(String[] args) {
        String root = args.length > 0 ? args[0] : "src/main/java";
        int x       = args.length > 1 ? Integer.parseInt(args[1]) : 3;

        var res = AnalysisRunner.analyze(root, x, "org.example.codebase");
        Map<String, Set<String>> full = res.stats().callGraph.asMap();
        Map<String, Set<String>> sub  = subgraphOfClasses(full, KEEP_CLASSES);

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Graphe d'appel (4 classes) — nœuds déplaçables");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            GraphPanel panel = new GraphPanel(sub);

            // barre haute
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            final JLabel lblInfo = new JLabel();
            final Runnable refreshInfo = () ->
                    lblInfo.setText("Nœuds: " + panel.nodes.size() + " | Arêtes: " + panel.edgeCount());
            refreshInfo.run();

            JButton btnFit   = new JButton("Adapter à la fenêtre");
            JButton btnReset = new JButton("Réinitialiser le layout");
            JButton btnPlus  = new JButton("+");
            JButton btnMoins = new JButton("−");

            btnFit.addActionListener(e -> { panel.fit(); panel.repaint(); refreshInfo.run(); });
            btnReset.addActionListener(e -> { panel.resetLayout(); panel.repaint(); refreshInfo.run(); });
            btnPlus.addActionListener(e -> { panel.scale *= 1.15; panel.repaint(); refreshInfo.run(); });
            btnMoins.addActionListener(e -> { panel.scale /= 1.15; panel.repaint(); refreshInfo.run(); });

            top.add(lblInfo); top.add(Box.createHorizontalStrut(12));
            top.add(btnFit); top.add(btnReset); top.add(btnPlus); top.add(btnMoins);

            f.add(top, BorderLayout.NORTH);
            f.setContentPane(panel);
            f.setSize(1100, 720);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    // ----- Filtrage: ne garder que les arêtes entièrement internes aux 4 classes -----
    private static Map<String, Set<String>> subgraphOfClasses(Map<String, Set<String>> adj, Set<String> keep) {
        Map<String, Set<String>> sub = new LinkedHashMap<>();
        for (var e : adj.entrySet()) {
            String caller = e.getKey();
            if (!belongsTo(caller, keep)) continue;
            Set<String> dst = sub.computeIfAbsent(caller, k -> new LinkedHashSet<>());
            for (String callee : e.getValue()) if (belongsTo(callee, keep)) dst.add(callee);
        }
        for (var e : adj.entrySet()) {
            for (String v : e.getValue()) if (belongsTo(v, keep)) sub.putIfAbsent(v, new LinkedHashSet<>());
        }
        return sub;
    }
    private static boolean belongsTo(String sig, Set<String> keep) {
        int idx = sig.indexOf('#');
        String cls = (idx >= 0) ? sig.substring(0, idx) : sig;
        return keep.contains(cls);
    }

    // ------------------------------ Panneau de dessin ------------------------------
    static class GraphPanel extends JPanel {
        final Map<String, Set<String>> adj;
        final List<String> nodes;
        final Map<String, Point> pos = new HashMap<>();          // centre de chaque bulle (coords monde)
        final Map<String, Rectangle> bounds = new HashMap<>();   // bbox pour hit-test (coords monde)

        float scale = 1.0f;
        int padX = 14, padY = 8, arc = 18;

        // pan
        int lastX, lastY;
        double offsetX = 0, offsetY = 0;

        // drag node
        String draggingNode = null;
        int dragDX = 0, dragDY = 0;

        GraphPanel(Map<String, Set<String>> adjacency) {
            this.adj = adjacency;
            this.nodes = new ArrayList<>(collectNodes(adjacency));
            setBackground(new Color(0xFAFAFA));

            addMouseWheelListener(e -> { scale *= (e.getPreciseWheelRotation() < 0) ? 1.1 : 1/1.1; repaint(); });

            MouseAdapter adapter = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    Point wm = toWorld(e.getPoint());
                    String hit = hitNode(wm.x, wm.y);
                    if (hit != null) {
                        draggingNode = hit;
                        Point c = pos.get(hit);
                        dragDX = wm.x - c.x;
                        dragDY = wm.y - c.y;
                    } else {
                        lastX = e.getX(); lastY = e.getY();
                    }
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (draggingNode != null) {
                        Point wm = toWorld(e.getPoint());
                        pos.put(draggingNode, new Point(wm.x - dragDX, wm.y - dragDY));
                    } else {
                        offsetX += (e.getX() - lastX);
                        offsetY += (e.getY() - lastY);
                        lastX = e.getX(); lastY = e.getY();
                    }
                    repaint();
                }
                @Override public void mouseReleased(MouseEvent e) { draggingNode = null; }
            };
            addMouseListener(adapter);
            addMouseMotionListener(adapter);
        }

        private Point toWorld(Point screen) {
            int wx = (int) Math.round((screen.x - offsetX) / scale);
            int wy = (int) Math.round((screen.y - offsetY) / scale);
            return new Point(wx, wy);
        }

        int edgeCount() { int s=0; for (var v: adj.values()) s+=v.size(); return s; }
        void fit(){ scale=1.0f; offsetX=offsetY=0; }
        void resetLayout(){ pos.clear(); bounds.clear(); layoutCircle(); }

        private static Set<String> collectNodes(Map<String, Set<String>> a){
            Set<String> s = new LinkedHashSet<>(a.keySet());
            for (Set<String> vs : a.values()) s.addAll(vs);
            return s;
        }

        @Override public void addNotify() {
            super.addNotify();
            layoutCircle();
        }

        /** Layout circulaire initial (n’affecte pas les nœuds déjà déplacés). */
        private void layoutCircle() {
            int n = Math.max(1, nodes.size());
            double w = Math.max(200, getWidth() - 80);
            double h = Math.max(200, getHeight() - 120);
            double r = 0.40 * Math.min(w, h);
            double cx = w / 2.0, cy = h / 2.0;

            for (int i=0;i<n;i++){
                String node = nodes.get(i);
                if (pos.containsKey(node)) continue; // déjà déplacé par l’utilisateur
                double ang = (2*Math.PI * i)/n - Math.PI/2;
                int x = (int)Math.round(cx + r*Math.cos(ang));
                int y = (int)Math.round(cy + r*Math.sin(ang));
                pos.put(node, new Point(x, y));
            }
        }

        private String hitNode(int wx, int wy) {
            ListIterator<String> it = nodes.listIterator(nodes.size());
            while (it.hasPrevious()) {
                String n = it.previous();
                Rectangle b = bounds.get(n);
                if (b != null && b.contains(wx, wy)) return n;
            }
            return null;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(1.3f));

            AffineTransform old = g2.getTransform();
            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);

            // Arêtes
            g2.setColor(new Color(0xB0BEC5));
            for (var e : adj.entrySet()) {
                Point p1 = pos.get(e.getKey());
                if (p1 == null) continue;
                for (String v : e.getValue()) {
                    Point p2 = pos.get(v);
                    if (p2 == null) continue;
                    drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
                }
            }

            // Nœuds
            bounds.clear();
            Font font = getFont().deriveFont(Font.PLAIN, 12f);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            for (String u : nodes) {
                Point p = pos.get(u); if (p == null) continue;
                String label = shorten(u);

                int textW = fm.stringWidth(label);
                int textH = fm.getAscent();
                int w = textW + padX*2;
                int h = textH + padY*2;
                int x = p.x - w/2;
                int y = p.y - h/2;

                bounds.put(u, new Rectangle(x, y, w, h));

                Shape bubble = new RoundRectangle2D.Double(x, y, w, h, arc, arc);
                g2.setColor(new Color(0xE3F2FD)); g2.fill(bubble);
                g2.setColor(new Color(0x1E88E5)); g2.draw(bubble);

                g2.setColor(new Color(0x0D47A1));
                g2.drawString(label, x + padX, y + padY + textH - 2);
            }

            g2.setTransform(old);
            g2.dispose();
        }

        private static String shorten(String sig){
            int i = sig.lastIndexOf('.');
            return (i >= 0) ? sig.substring(i+1) : sig; // "Classe#methode"
        }

        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
            double phi = Math.toRadians(25);
            int barb = 10;
            double dy = y2 - y1, dx = x2 - x1;
            double theta = Math.atan2(dy, dx);
            double x, y, rho = theta + phi;
            for (int j=0;j<2;j++){
                x = x2 - barb*Math.cos(rho);
                y = y2 - barb*Math.sin(rho);
                g2.draw(new Line2D.Double(x2, y2, x, y));
                rho = theta - phi;
            }
        }
    }
}
