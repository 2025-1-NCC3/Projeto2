const QUESTIONS = [
    { id: 1, text: "Você costuma ansiar por emoção?" },
    { id: 2, text: "Você geralmente é despreocupado?" },
    { id: 3, text: "Você para e pensa sobre as coisas antes de fazer qualquer coisa?" },
    { id: 4, text: "De repente você se sente tímido quando quer conversar com um estranho atraente?" },
    { id: 5, text: "Geralmente você prefere ler a conhecer pessoas?" },
    { id: 6, text: "Seu humor sobe e desce?" },
    { id: 7, text: "Você já se sentiu \"apenas infeliz\" sem um bom motivo?" },
    { id: 8, text: "De vez em quando você perde a paciência e fica com raiva?" },
    { id: 9, text: "Você costuma se preocupar com coisas que deveria ter feito ou dito?" },
    { id: 10, text: "Seus sentimentos são facilmente magoados?" },
    { id: 11, text: "Você acha difícil aceitar um NÃO como resposta?" },
    { id: 12, text: "Você costuma fazer e dizer coisas rapidamente, sem parar para pensar?" },
    { id: 13, text: "Você faria quase qualquer coisa por um desafio?" },
    { id: 14, text: "Você costuma fazer coisas no calor do momento?" },
    { id: 15, text: "Ocasionalmente, você tem pensamentos e ideias que não gostaria que outras pessoas soubessem?" }
];

export function getAll(req, res) {
    res.status(201).json(QUESTIONS);
}