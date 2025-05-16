import CryptoJS from 'crypto-js';

const SECRET_KEY = "1234567890123456"; // Mesma chave do Android

export const decryptSurvey = (req, res, next) => {
  try {
    const { iv, data } = req.body;
    
    const decrypted = CryptoJS.AES.decrypt(data, SECRET_KEY, {
      iv: CryptoJS.enc.Base64.parse(iv)
    });
    
    req.surveyData = JSON.parse(decrypted.toString(CryptoJS.enc.Utf8));
    next();
  } catch (error) {
    console.error('Decryption error:', error);
    res.status(400).json({ error: 'Falha na descriptografia dos dados' });
  }
};