# TP1 – Analyse Statique (Partie 2)

## 1️. Cloner le dépôt
```bash
git clone https://github.com/Ines-Amirat/Static-analysis-TP.git
cd Static-analysis-TP
```
## 2️. Technologies utilisées
  - Java 17
  - Maven
  - Eclipse JDT (AST)
  - Swing
  - spoon
    
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

### CouplingAppMain.java — Graphe de couplage entre classes
- **Objectif :** Construire le graphe de couplage directionnel entre classes à partir du graphe d’appels.
- **Exécution :** Depuis l’IDE : lancer org.analysis.CouplingAppMain

- **Sortie :**
  - Console : affiche T et chaque arête A->B : w=.. (c=..) triée.
  - UI Swing (fenêtre) : 
    - graphe pondéré 
    - Nœuds = classes 
    - Arêtes orientées avec épaisseur ∝ w et label w=… (c=…)

### ModulesAppMain.java — Clustering hiérarchique & modules
- **Objectif :** Regrouper les classes via un clustering hiérarchique agglomératif (average linkage) avec similarité
- **Exécution :** Depuis l’IDE : lancer org.analysis.ModulesAppMain

- **Sortie :**
    - Console : liste des modules retenus avec leur taille et moyenne interne.
    - UI Swing : dendrogramme horizontal (branches lissées, couleur par niveau, zoom/pan) + panneau “Modules extraits” à droite. 

## 4. Analyse par Spoon
### CouplingAppMainSpoon.java — Coupling
- **Objectif :** construire le graphe d’appels avec Spoon, agréger classe→classe et calculer Couplage(A→B)=Tw(A→B)
- **Sortie :**
  - Console : total T + chaque arête A->B : w=.. (c=..) triée.
  - UI Swing : graphe de couplage pondéré (nœuds=classes, arêtes orientées avec épaisseur ∝ w, label w=.. (c=..), drag/pan/zoom).

### ModulesAppMainSpoon.java — Clustering
- **Objectif :** faire un clustering hiérarchique (average linkage) et extraire les modules (règles : ≤ M/2 modules, même branche du dendrogramme, moyenne interne > CP).
- **Sortie :**
  - Console : liste des modules retenus (taille, moyenne de couplage interne). 
  - UI Swing : dendrogramme horizontal (zoom/pan) + panneau “Modules extraits”.

## 5. Photos d'execution

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/f5406c62-94af-49f6-93ae-f6eeccd9ed47" />
<img width="977" height="641" alt="image" src="https://github.com/user-attachments/assets/bdb65224-4095-4e4b-a950-8468818ad433" />
<img width="700" height="838" alt="image" src="https://github.com/user-attachments/assets/2993e714-fbe1-4e8a-8471-9153fd151ac3" />
<img width="1283" height="418" alt="image" src="https://github.com/user-attachments/assets/7023fd8c-9d6c-461b-a92e-f09cd0a113f6" />

