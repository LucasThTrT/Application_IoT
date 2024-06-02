// Importation des fonctions fft et util de la bibliothèque fft-js
const fft = require('fft-js').fft;
const fftUtil = require('fft-js').util;

// Définition de la fonction handler asynchrone exportée, qui sera exécutée lorsque l'événement est reçu
exports.handler = async (event) => {
  let steps = 1; // Initialisation du compteur
  try {
    // Log de l'événement reçu
    console.log('1. Event received:', event);

    // Parse le corps de l'événement pour obtenir les données
    const body = JSON.parse(event.body);
    console.log('2. Parsed body:', body);

    // Extraction des données d'accélération verticale du corps
    const accelerationData = body.verticalAccelerations;
    console.log('3. Acceleration data:', accelerationData);

    // Vérifie que les données d'accélération sont un tableau
    if (!Array.isArray(accelerationData)) {
      throw new Error('Invalid acceleration data'); // Lance une erreur si les données ne sont pas un tableau
    }
    console.log('4. Valid array');

    // Convertit les données d'accélération en nombres flottants
    const accelerationArray = accelerationData.map(parseFloat);

    // Suppression de la composante continue (DC) en soustrayant la moyenne de chaque valeur
    const mean = accelerationArray.reduce((acc, val) => acc + val, 0) / accelerationArray.length;
    const centeredAccelerationData = accelerationArray.map(val => val - mean);

    // Application de la FFT aux données d'accélération centrées
    const phasors = fft(centeredAccelerationData);
    console.log('5. Phasors:', phasors);

    // Calcul des magnitudes des phasors
    const magnitudes = phasors.map(ph => Math.sqrt(ph[0] ** 2 + ph[1] ** 2));
    console.log('6. Magnitudes:', magnitudes);

    // Détermination de la fréquence d'échantillonnage et de la taille de la FFT à partir du corps
    const sampleRate = 50; // 50Hz
    const fftSize = 2500;   // mesures
    
    // Définition des fréquences minimale et maximale d'intérêt (en Hz)
    const minFrequency = 1;  // Fréquence minimale d'intérêt (Hz)
    const maxFrequency = 3;  // Fréquence maximale d'intérêt (Hz)

    // Calcul des indices minimaux et maximaux correspondant aux fréquences d'intérêt
    const minIndex = Math.ceil(minFrequency * fftSize / sampleRate);
    const maxIndex = Math.floor(maxFrequency * fftSize / sampleRate);

    // Initialisation des variables pour trouver la magnitude maximale
    let maxIndexValue = -1;
    let maxMagnitude = -Infinity;

    // Parcours des magnitudes dans la plage d'indices d'intérêt pour trouver la magnitude maximale
    for (let i = minIndex; i <= maxIndex; i++) {
      if (magnitudes[i] > maxMagnitude) {
        maxMagnitude = magnitudes[i];
        maxIndexValue = i;
      }
    }

    // Log de l'indice et de la magnitude maximaux trouvés
    console.log('Max index:', maxIndexValue);
    console.log('Max magnitude:', maxMagnitude);

    // Si une magnitude maximale valide est trouvée, calcul de la fréquence fondamentale
    if (maxIndexValue !== -1) {
      const fundamentalFrequency = maxIndexValue * sampleRate / fftSize;
      console.log('Fundamental frequency:', fundamentalFrequency);

      // Calcul de la cadence (nombre de pas par seconde) à partir de la fréquence fondamentale
      const cadence = fundamentalFrequency; // steps per second
      const timeElapsed = body.time; // temps en secondes
      steps = Math.round(cadence * timeElapsed); // Calcul du nombre total de pas
      console.log('Calculated steps:', steps);
    }

    // Retourne le nombre de pas calculé avec un code de statut 200
    return {
      statusCode: 200,
      body: JSON.stringify({ steps: steps }),
    };

  } catch (error) {
    // Log de l'erreur et retour d'un message d'erreur avec un code de statut 500
    console.error('Error processing request:', error);
    return {
      statusCode: 500,
      body: JSON.stringify({ steps: steps}),
      //body: JSON.stringify({ error: error.message }),
    };
  }
};
