package org.analysis.gui;


import org.analysis.processing.StatisticsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AppFrame extends JFrame {

    private final JTextField tfRoot = new JTextField("src/main/java/org/example/codebase");
    private final JTextField tfX = new JTextField("3");
    private final JTextField tfPkg = new JTextField("org.example.codebase");
    private final JButton btnParcourir = new JButton("Parcourir…");
    private final JButton btnAnalyser = new JButton("Analyser");

    private final String[] questions = new String[]{
            "1) Nombre de classes",
            "2) Nombre de lignes de code (LOC)",
            "3) Nombre total de méthodes",
            "4) Nombre total de packages",
            "5) Nombre moyen de méthodes par classe",
            "6) Nombre moyen de LOC par méthode",
            "7) Nombre moyen d'attributs par classe",
            "8) Top 10% des classes (par # méthodes)",
            "9) Top 10% des classes (par # attributs)",
            "10) Intersection des deux tops",
            "11) Classes avec > X méthodes",
            "12) Top 10% des méthodes (LOC) par classe",
            "13) Nombre maximal de paramètres (global)",
            "14) Graphe d'appel (liste d'adjacence)"
    };
    private final JComboBox<String> cbQuestion = new JComboBox<>(questions);
    private final JTextArea taResult = new JTextArea(16, 80);

    // garde la dernière analyse en mémoire
    private StatisticsService.Answers answers = null;

    public AppFrame() {
        super("Analyse Statique – Menu des questions");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);

        taResult.setEditable(false);
        taResult.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JPanel north = new JPanel(new GridBagLayout());
        north.setBorder(new EmptyBorder(12, 12, 6, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.fill = GridBagConstraints.HORIZONTAL;

        int col = 0;
        c.gridx = col++; c.gridy = 0; north.add(new JLabel("Dossier source :"), c);
        c.gridx = col++; c.weightx = 1.0; north.add(tfRoot, c);
        c.gridx = col++; c.weightx = 0; north.add(btnParcourir, c);

        col = 0;
        c.gridx = col++; c.gridy = 1; north.add(new JLabel("X (Q11) :"), c);
        c.gridx = col++; north.add(tfX, c);
        c.gridx = col++; north.add(new JLabel("Filtre package (optionnel) :"), c);
        c.gridx = col++; c.weightx = 1.0; north.add(tfPkg, c);
        c.gridx = col++; c.weightx = 0; north.add(btnAnalyser, c);

        JPanel center = new JPanel(new BorderLayout(8,8));
        center.setBorder(new EmptyBorder(6, 12, 12, 12));
        JPanel topLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topLine.add(new JLabel("Question :"));
        topLine.add(cbQuestion);
        JButton btnAfficher = new JButton("Afficher le résultat");
        topLine.add(btnAfficher);

        center.add(topLine, BorderLayout.NORTH);
        center.add(new JScrollPane(taResult,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // Actions
        btnParcourir.addActionListener(this::chooseFolder);
        btnAnalyser.addActionListener(this::runAnalysis);
        btnAfficher.addActionListener(e -> showSelectedQuestion());
        cbQuestion.addActionListener(e -> { if (answers != null) showSelectedQuestion(); });
    }

    private void chooseFolder(ActionEvent e) {
        JFileChooser fc = new JFileChooser(tfRoot.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int r = fc.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            tfRoot.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void runAnalysis(ActionEvent e) {
        try {
            String root = tfRoot.getText().trim();
            String pkg = tfPkg.getText().trim();
            int x = Integer.parseInt(tfX.getText().trim());

            answers = AnalysisRunner.analyze(root, x, pkg.isBlank() ? null : pkg).answers();
            JOptionPane.showMessageDialog(this, "Analyse terminée ✅", "Info", JOptionPane.INFORMATION_MESSAGE);
            showSelectedQuestion();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSelectedQuestion() {
        if (answers == null) {
            taResult.setText("Veuillez d’abord cliquer sur « Analyser ».");
            return;
        }
        int idx = cbQuestion.getSelectedIndex();
        String out;
        switch (idx) {
            case 0 -> out = String.valueOf(answers.nbClasses());
            case 1 -> out = String.valueOf(answers.nbLOC());
            case 2 -> out = String.valueOf(answers.nbMethods());
            case 3 -> out = String.valueOf(answers.nbPackages());
            case 4 -> out = String.valueOf(answers.avgMethodsPerClass());
            case 5 -> out = String.valueOf(answers.avgLocPerMethod());
            case 6 -> out = String.valueOf(answers.avgFieldsPerClass());
            case 7 -> out = answers.top10pctByMethods().toString();
            case 8 -> out = answers.top10pctByFields().toString();
            case 9 -> out = answers.intersectionTop10pct().toString();
            case 10 -> out = answers.classesMoreThanXMethods().toString();
            case 11 -> {
                var sb = new StringBuilder();
                answers.top10pctLongestMethodsPerClass().forEach((cls, list) ->
                        sb.append("• ").append(cls).append(" -> ").append(list).append('\n'));
                out = sb.toString();
            }
            case 12 -> out = String.valueOf(answers.maxParameters());

            default -> out = "";
        }

        taResult.setText("== " + questions[idx] + " ==\n" + out);
        taResult.setCaretPosition(0);
    }
}
