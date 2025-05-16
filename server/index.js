import express, { json } from 'express';
import cors from 'cors';
import sequelize from './src/config/database.js';
import authRoute from './src/routes/auth.js';
import questionsRoutes from './src/routes/question.js';
import surveyRoutes from './src/routes/survey.js';
import userRoutes from './src/routes/user.js';
import models from './src/models/index.js';

const app = express();

app.use(cors());
app.use(json());
app.use('/auth', authRoute);
app.use('/questions', questionsRoutes);
app.use('/survey', surveyRoutes);
app.use('/user', userRoutes);

sequelize.sync().then(() => {
  app.listen(3000, () => {
    console.log('DB and Server status OK, port:3000 ');
  });
}).catch((error) => console.error("Erro na sincronização:", error));