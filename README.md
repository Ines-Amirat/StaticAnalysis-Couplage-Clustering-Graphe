# TP1 – Analyse Statique (Partie 2)

## 1️. Cloner le dépôt
```bash
git clone https://github.com/Ines-Amirat/Static-analysis-TP.git
cd Static-analysis-TP
```
## 2️. Technologies utilisées
  - Java 21
  - Maven
  - Eclipse JDT (AST)
  - Swing
    
## 3. Ouvrir le projet dans un IDE

  1. Ouvrir le projet dans IntelliJ IDEA ou Eclipse.
  2. Vérifier que le JDK 17 (ou supérieur) est bien configuré.
  3. S’assurer que le dossier source est src/main/java.

## 3. Points d’exécution principaux
### AppSwing.java — Question 1 : Statistiques
- **But :**  lance l’interface graphique Swing de l’application pour choisir un dossier, exécuter l’analyse du code source et afficher les résultats des métriques dans une fenêtre.
  
- **Exécution :**
  - Lancer la classe 
  - Une fenêtre s’ouvre permettant de sélectionner le dossier source du code Java.
  - Choisir la profondeur d’analyse.
  - Cliquer sur button Analyser.
  - Puis afficher les résultats selon la question choisie (ex. nombre de classes, méthodes, lignes de code, etc.).

- **Sortie :**  
Affichage clair des métriques dans une interface Swing.

### CallGraphGuiApp.java — Question 2 : Graphe d’appel
- **But :**  afficher le graphe orienté des appels de méthodes entre classes analysées.

- **Exécution :**
  - Lancer la classe
  - Une **fenêtre Swing** s’ouvre 
  - chaque **nœud** représente une méthode (`Class#method`)
  - les **flèches orientées** montrent les appels entre méthodes

- **Sortie :**  
Une visualisation interactive du graphe d’appel.

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/f5406c62-94af-49f6-93ae-f6eeccd9ed47" />
<img width="977" height="641" alt="image" src="https://github.com/user-attachments/assets/bdb65224-4095-4e4b-a950-8468818ad433" />


