import models from './src/models/index.js';
import sequelize from './src/config/database.js';

const questions = [
    {
        question: "Como você se sente em grandes grupos sociais?",
        alternatives: ["Muito confortável", "Confortável", "Neutro", "Desconfortável", "Muito desconfortável"]
    },
    {
        question: "Qual dessas atividades você prefere?",
        alternatives: ["Festa animada", "Cinema", "Leitura", "Esportes", "Jogos online"]
    },
    {
        question: "Como você reage a mudanças inesperadas?",
        alternatives: ["Adoro novidades", "Me adapto bem", "Neutro", "Prefiro rotina", "Fico ansioso"]
    }
];

async function seedDatabase() {
    try {
        await sequelize.sync({ force: true });
        await models.Question.bulkCreate(questions);

        console.log('Succesfully populated!');
        process.exit(0);
    } catch (error) {
        console.error('Error seed: ', error);
        process.exit(1);
    }
}

seedDatabase();