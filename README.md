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

## Compilation et exécution

```bash
# Compiler le projet (produit target/video-scramble.jar)
mvn clean package
```

```bash
# Lancer l'application
make run
# ou
mvn clean javafx:run
```

L'application démarre en mode graphique. L'utilisateur choisit la vidéo source, l'algorithme de chiffrement et la clé via l'interface.

## Types de brouillage

### Discret 11 (Retard des lignes)

[Wikipedia - Discret 11](https://fr.wikipedia.org/wiki/Discret_11)

Ce système utilisé par Canal+ appliquait un retard temporel (un décalage horizontal) à chaque ligne.

- Il y avait trois possibilités de retard : 0 nanoseconde (pas de décalage), 902 nanosecondes ou 1804 nanosecondes.
- Le choix du décalage pour chaque ligne était dicté par une séquence pseudo-aléatoire générée à partir du code secret tapé sur le clavier. L'image apparaissait "hachée" horizontalement.
- Le son était quant à lui inversé en fréquence (les sons aigus devenaient graves et inversement), donnant l'impression que les gens parlaient une langue extraterrestre.

### Nagravision

[Cricrac - Algorithm Nagravision](http://cricrac.free.fr/download/Doc/ALGORITH.HTM)
Système du décodeur à clé (la clé blanche puis marron). Au lieu de décaler les lignes horizontalement, ce système les mélangeait verticalement.

- La ligne 10 pouvait être affichée à la place de la ligne 50, la 11 à la place de la 120, etc.
- Le mélange changeait très rapidement selon un algorithme complexe, dont la "recette" pour remettre les lignes dans l'ordre était calculée par la puce intégrée dans la clé du client.
