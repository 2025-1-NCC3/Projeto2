import models from './src/models/index.js';
import sequelize from './src/config/database.js';

const questions = [
    {
        question: "Você costuma ansiar por emoção?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "Você geralmente é despreocupado?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "Você para e pensa sobre as coisas antes de fazer qualquer coisa?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "De repente você se sente tímido quando quer conversar com um estranho atraente?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "Geralmente você prefere ler a conhecer pessoas?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "Seu humor sobe e desce?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "Você já se sentiu 'apenas infeliz' sem um bom motivo?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "De vez em quando você perde a paciência e fica com raiva?",
        alternatives: ["Sim" , "Não"]
    },   
    {
        question: "Você costuma se preocupar com coisas que deveria ter feito ou dito?",
        alternatives: ["Sim" , "Não"]
    }, 
    {
        question: "Seus sentimentos são facilmente magoados?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "Você acha difícil aceitar um NÃO como resposta?",
        alternatives: ["Sim" , "Não"]
    }, 
    {
        question: "Você costuma fazer e dizer coisas rapidamente, sem parar para pensar??",
        alternatives: ["Sim" , "Não"]
    }, 
    {
        question: "Você faria quase qualquer coisa por um desafio?",
        alternatives: ["Sim" , "Não"]
    },
    {
        question: "Você costuma fazer coisas no calor do momento?",
        alternatives: ["Sim" , "Não"]
    },   
    {
        question: "Ocasionalmente, você tem pensamentos e ideias que não gostaria que outras pessoas soubessem?",
        alternatives: ["Sim" , "Não"]
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