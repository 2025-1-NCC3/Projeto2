import models from "../models/index.js";

export default {
    async getAll(req, res) {
        try {
            const questions = await models.Question.findAll({
                attributes: ['id', 'question', 'alternatives']
            });
            res.status(200).json(questions);
        } catch (error) {
            console.error("Erro ao buscar perguntas:", error);
            res.status(500).json({ error: "Falha ao buscar perguntas" });
        }
    },

    async getById(req, res) {
        try {
            const question = await models.Question.findByPk(req.params.id, {
                attributes: ['id', 'question', 'alternatives']
            });

            if (!question) {
                return res.status(404).json({ error: "Pergunta não encontrada" });
            }

            res.status(200).json(question);
        } catch (error) {
            console.error("Erro ao buscar pergunta:", error);
            res.status(500).json({ error: "Falha ao buscar pergunta" });
        }
    }
};