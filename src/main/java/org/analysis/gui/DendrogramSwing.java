package org.analysis.gui;

import org.analysis.clustering.ClusterNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/**
 * Dendrogramme "pretty" :
 *  - Branches lissées (courbes) avec couleur par niveau
 *  - Étiquettes en pastilles (pills) pour les feuilles
 *  - Fond quadrillé doux
 *  - Zoom (molette) & Pan (drag)
 *  - Échelle de hauteur h=... sur la gauche
 *
 * API identique à l’ancienne : DendrogramSwing.show(root, modulesText)
 */
public class DendrogramSwing extends JPanel {

    private final ClusterNode root;

    // Layout
    private int maxDepth;
    private final Map<ClusterNode, Integer> depth = new HashMap<>();
    private final Map<ClusterNode, Point2D.Double> anchor = new HashMap<>(); // “points parent”
    private final java.util.List<ClusterNode> leavesInOrder = new ArrayList<>();

    // Style
    private static final int LEAF_GAP = 34;     // écart vertical entre feuilles
    private static final int LEVEL_GAP = 100;   // écart horizontal entre niveaux
    private static final int LEFT_MARGIN = 80;  // pour l’échelle h=…
    private static final int TOP_MARGIN  = 40;
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    // Interactions
    private double scale = 1.0;
    private int offX = 0, offY = 0;
    private Point lastDrag;

    public DendrogramSwing(ClusterNode root) {
        this.root = root;
        setBackground(Color.WHITE);
        computeLayoutInfo();
        setPreferredSize(new Dimension(
                LEFT_MARGIN + (maxDepth + 1) * LEVEL_GAP + 400,
                Math.max(500, TOP_MARGIN + leavesInOrder.size() * LEAF_GAP + 100)
        ));
        enableInteractions();
    }

    /* =================== interactions =================== */
    private void enableInteractions() {
        addMouseWheelListener(e -> {
            double f = (e.getWheelRotation() < 0) ? 1.1 : 1/1.1;
            scale = Math.max(0.5, Math.min(3.0, scale * f));
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { lastDrag = e.getPoint(); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (lastDrag != null) {
                    offX += e.getX() - lastDrag.x;
                    offY += e.getY() - lastDrag.y;
                    lastDrag = e.getPoint();
                    repaint();
                }
            }
        });
    }

    /* =================== layout =================== */
    private void computeLayoutInfo() {
        // profondeur + ordre des feuilles (gauche->droite)
        maxDepth = 0;
        dfsDepth(root, 0);
        collectLeaves(root);
        // placer chaque feuille en Y régulier ; X par profondeur
        for (int i = 0; i < leavesInOrder.size(); i++) {
            ClusterNode leaf = leavesInOrder.get(i);
            double x = LEFT_MARGIN + depth.get(leaf) * LEVEL_GAP;
            double y = TOP_MARGIN + i * LEAF_GAP;
            anchor.put(leaf, new Point2D.Double(x, y));
        }
        // puis propager les ancres vers le haut : y-parent = milieu(yL, yR)
        propagateAnchors(root);
    }

    private void dfsDepth(ClusterNode n, int d) {
        depth.put(n, d);
        maxDepth = Math.max(maxDepth, d);
        if (!n.isLeaf()) {
            dfsDepth(n.left, d + 1);
            dfsDepth(n.right, d + 1);
        }
    }

    private void collectLeaves(ClusterNode n) {
        if (n.isLeaf()) {
            leavesInOrder.add(n);
        } else {
            collectLeaves(n.left);
            collectLeaves(n.right);
        }
    }

    private void propagateAnchors(ClusterNode n) {
        if (n.isLeaf()) return;
        propagateAnchors(n.left);
        propagateAnchors(n.right);
        Point2D.Double L = anchor.get(n.left);
        Point2D.Double R = anchor.get(n.right);
        double x = LEFT_MARGIN + depth.get(n) * LEVEL_GAP;
        double y = (L.y + R.y) / 2.0;
        anchor.put(n, new Point2D.Double(x, y));
    }

    /* =================== rendu =================== */
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // transform
        g2.translate(offX, offY);
        g2.scale(scale, scale);

        // qualité
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // fond quadrillé doux
        drawGrid(g2);

        // échelle de hauteur h sur la gauche
        drawHeightScale(g2);

        // branches
        drawBranches(g2, root);

        // feuilles (labels)
        drawLeafLabels(g2);

        g2.dispose();
    }

    private void drawGrid(Graphics2D g2) {
        Rectangle clip = g2.getClipBounds();
        if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(240, 244, 248));
        for (int x = 0; x < clip.width / scale + 2000; x += 40)
            g2.drawLine(x, -5000, x, 5000);
        for (int y = -5000; y < clip.height / scale + 2000; y += 40)
            g2.drawLine(-5000, y, 5000, y);
    }

    private void drawHeightScale(Graphics2D g2) {
        // Dessine quelques repères h=0.0, 0.25, 0.5, 0.75, 1.0
        g2.setColor(new Color(0x6B7785));
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        for (double h = 0.0; h <= 1.0001; h += 0.25) {
            String txt = String.format("h=%.2f", h);
            g2.drawString(txt, 10, (int)(TOP_MARGIN + h * 200)); // échelle visuelle indicative
        }
    }

    private void drawBranches(Graphics2D g2, ClusterNode n) {
        if (n.isLeaf()) return;

        Point2D.Double P = anchor.get(n);
        Point2D.Double L = anchor.get(n.left);
        Point2D.Double R = anchor.get(n.right);

        // couleur selon profondeur
        int d = depth.get(n);
        Color col = levelColor(d);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(col);

        // Dessiner deux courbes douces depuis P -> L et P -> R (QuadCurve)
        double ctrlX = P.x + LEVEL_GAP * 0.55;
        QuadCurve2D q1 = new QuadCurve2D.Double(P.x, P.y, ctrlX, L.y, L.x, L.y);
        QuadCurve2D q2 = new QuadCurve2D.Double(P.x, P.y, ctrlX, R.y, R.x, R.y);
        g2.draw(q1);
        g2.draw(q2);

        // Etiquette de hauteur à gauche du point P
        g2.setColor(new Color(0x5A6B7C));
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g2.drawString(String.format("h=%.2f", n.height), (int)P.x - 48, (int)P.y - 6);

        // récursif
        drawBranches(g2, n.left);
        drawBranches(g2, n.right);
    }

    private Color levelColor(int level) {
        // palette douce bleutée en fonction du niveau
        int base = 0x2D6CDF;
        float a = 0.45f + Math.min(0.4f, level * 0.06f);
        return new Color((base >> 16) & 0xFF, (base >> 8) & 0xFF, base & 0xFF, (int)(a * 255));
    }

    private void drawLeafLabels(Graphics2D g2) {
        g2.setFont(LABEL_FONT);
        for (ClusterNode leaf : leavesInOrder) {
            Point2D.Double p = anchor.get(leaf);
            drawPill(g2, (int)p.x, (int)p.y, leaf.items.get(0));
        }
    }

    private void drawPill(Graphics2D g2, int x, int y, String text) {
        FontMetrics fm = g2.getFontMetrics();
        int w = Math.max(60, fm.stringWidth(text) + 18);
        int h = 26;

        int rx = x + 30;               // décalage à droite du nœud
        int ry = y - h/2;

        // ombre légère
        g2.setColor(new Color(0,0,0,30));
        g2.fillRoundRect(rx+2, ry+2, w, h, 16, 16);

        // fond
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(rx, ry, w, h, 16, 16);

        // bord
        g2.setColor(new Color(0x2D6CDF));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(rx, ry, w, h, 16, 16);

        // point
        g2.setColor(new Color(0x2D6CDF));
        g2.fillOval(rx + 8 - 4, ry + h/2 - 4, 8, 8);

        // label
        g2.setColor(new Color(0x0F1C2E));
        g2.drawString(text, rx + 16, ry + (h + fm.getAscent() - fm.getDescent())/2);
    }

    /* =================== fenêtre =================== */
    public static void show(ClusterNode root, java.util.List<String> modulesText) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Dendrogramme et modules");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel right = new JPanel(new BorderLayout());
            JTextArea ta = new JTextArea();
            ta.setEditable(false);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            ta.setText(String.join("\n", modulesText));
            right.add(header("Modules extraits"), BorderLayout.NORTH);
            right.add(new JScrollPane(ta), BorderLayout.CENTER);
            right.setPreferredSize(new Dimension(420, 640));

            JScrollPane leftScroll = new JScrollPane(new DendrogramSwing(root));
            leftScroll.getHorizontalScrollBar().setUnitIncrement(20);
            leftScroll.getVerticalScrollBar().setUnitIncrement(20);

            JSplitPane split = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    leftScroll,
                    right
            );
            split.setResizeWeight(1.0);

            f.getContentPane().add(split);
            f.setSize(1280, 760);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    private static JComponent header(String title) {
        JLabel l = new JLabel(title);
        l.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        JPanel p = new JPanel(new BorderLayout());
        p.add(l, BorderLayout.CENTER);
        p.setBackground(new Color(0xF3F5F7));
        p.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(0xE2E7EC)));
        return p;
    }
}
