# Projet IoT et Remote Processing : Serveur podométrique

## 2SN-R, 2022-2023


## Objectif
Ce projet vise à nous familiariser avec la manipulation des données des capteurs, leur envoi sur un réseau sans fil et leur traitement sur une plateforme cloud. Pour cela, nous allons développer un podomètre, une application qui permet de compter le nombre de pas effectués par l'utilisateur. Le dispositif de collecte de données sera le smartphone, la connectivité sera assurée par un simple point d’accès wifi, et l’essentiel du traitement, incluant le calcul du nombre de pas, sera fait par une application web implémentée sur un serveur (un PC connecté au point d’accès).

## Description
Chaque utilisateur dispose d’un smartphone exécutant une application mobile qui collecte périodiquement les données des capteurs de ce smartphone et les envoie à une plateforme de cloud pour traitement. Cette plateforme calcule en temps réel le nombre de pas effectués par chaque utilisateur. Nous nous concentrerons uniquement sur le comptage de pas.

Notre rôle est de développer à la fois l’application mobile et la plateforme de traitement centralisé, avec une connexion wifi simplifiée. La partie mobile sera implantée en Android (Java) et la partie distante en tant qu’application Web sur AWS.

### Calcul du nombre de pas
Pour calculer le nombre de pas faits pendant une période de temps \( t \) :
1. **Cadence de l’utilisateur** : Appliquer la transformée de Fourier sur le signal de l’accélération verticale et rechercher la fréquence fondamentale de ce signal (fréquence avec la plus grande amplitude).
2. **Nombre de pas** : Multiplier la cadence par la durée \( t \).

## Technologies utilisées
### Côté client (application mobile)
- Développée en Android natif (Java)

### Côté serveur
- Application Web JEE sur AWS
- Servlet principale pour calculer le nombre de pas à partir des données reçues
- Librairie « Apache Commons Math » pour la transformée de Fourier

## Livrables
- Rapport de projet incluant une description du fonctionnement du système (architectures, mécanismes de communication, etc.)
- Codes sources

## Suivi du projet
1. **Séance 1** : Présentation du projet, prise en main des outils de développement et conception architecturale de la solution
2. **Séances 2, 3 et 4** : Suivi de l’avancement et réponses aux questions
3. **Séance 5** : Évaluation

### Évaluation
- Test grandeur nature avec au moins 2 téléphones (2 utilisateurs)
- Points clés évalués : récupération des données des capteurs, envoi en temps réel au serveur distant, obtention des résultats en temps réel, qualité des résultats
