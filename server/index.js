import express, { json } from 'express';
import cors from 'cors';
import sequelize from './src/config/database.js';
import authRoute from './src/routes/auth.js';
import questionsRoute from './src/routes/question.js';
import surveysRoute from './src/routes/survey.js';
import models from './src/models/index.js';

const app = express();

app.use(cors());
app.use(json());
app.use('/auth', authRoute);
app.use('/question', questionsRoute);
app.use('/survey', surveysRoute);

sequelize.sync().then(() => {
  app.listen(3000, () => {
    console.log('DB and Server status OK, port:3000 ');
  });
}).catch((error) => console.error("Erro na sincronização:", error));