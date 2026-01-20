# VideoScramble

Application JavaFX pour le chiffrement et déchiffrement de vidéos.

## Prérequis

- **Java** 23 ou supérieur
- **Maven** 3.6+
- JavaFX est inclus automatiquement via Maven

### Pourquoi Maven ?

Les dépendances opencv et javafx causent de multiples problèmes d'exécution selon les version disponibles localement.
Dans le cadre d'un projet n'ayant aucune contrainte d'espace, nous allons les embarquer dans le jar.

> 💡 Maven permet aussi de simplifier le processus de récupération des dépendances sans dépendre d'un IDE.

## Compilation du projet

### Générer le JAR exécutable

Pour compiler le projet et créer un JAR exécutable avec toutes les dépendances, utilisez la commande suivante :

```bash
mvn clean package
```

Cette commande va :
1. Nettoyer les fichiers de compilation précédents
2. Compiler les sources
3. Créer un JAR exécutable contenant toutes les dépendances dans `target/video-scramble.jar`

Le JAR généré se trouve dans le répertoire `target/` et porte le nom `video-scramble.jar`.

## Exécution de l'application

### Via le JAR généré

L'application nécessite trois arguments obligatoires :
- `width` : largeur de la vidéo (en pixels)
- `height` : hauteur de la vidéo (en pixels)
- `file` : chemin vers le fichier vidéo à traiter

Exemple d'utilisation :

```bash
java -jar target/video-scramble.jar 1280 720 video/Pencil_Candle_1280x720.mp4
```

### Via Maven (javafx:run)

Le plus simple est d'utiliser le plugin Maven JavaFX pour lancer directement l'application :

```bash
mvn javafx:run
```

Cette commande utilise les arguments configurés dans le `pom.xml` par défaut :
- `1280` (largeur)
- `720` (hauteur)
- `video/Pencil_Candle_1280x720.mp4` (fichier vidéo)

Pour spécifier vos propres arguments, modifiez la propriété `app.arg` dans le `pom.xml` :

```xml
<properties>
    <app.arg>1920 1080 chemin/vers/votre/video.mp4</app.arg>
</properties>
```

Ou lancez la commande avec des arguments personnalisés (si votre plugin Maven le supporte) :

```bash
mvn javafx:run -Djavafx.run.args="1920 1080 video/ma_video.mp4"
```
