import models from "../models/index.js";
import jwt from "jsonwebtoken";
import 'dotenv/config'

export default {
    async create(req, res) {
        try {
            const token = req.headers.authorization?.split(' ')[1];
            if (!token) return res.status(401).json({ error: "Token não fornecido" });

            const decoded = jwt.verify(token, process.env.JWT_SECRET);
            const user = await models.User.findByPk(decoded.id);
            if (!user) return res.status(404).json({ error: "Usuário não encontrado" });

            const question = await models.Question.findByPk(req.body.questionId);
            if (!question) return res.status(404).json({ error: "Pergunta não existe" });

            const newSurvey = await models.Survey.create({
                questionId: req.body.questionId,
                answer: req.body.answer,
                userId: decoded.id
            });

            res.status(201).json(newSurvey);

        } catch (error) {
            console.error("Erro detalhado:", {
                message: error.message,
                body: req.body,
                sql: error.parent?.sql
            });

            res.status(400).json({
                error: "Erro ao salvar resposta",
                details: error.message
            });
        }
    },

    async getAllByUser(req, res) {
        try {
            const token = req.headers.authorization?.split(' ')[1];
            if (!token) {
                return res.status(401).json({ error: "Acesso não autorizado" });
            }

            const decoded = jwt.verify(token, process.env.JWT_SECRET);

            const surveys = await models.Survey.findAll({
                where: { userId: decoded.id },
                include: [
                    {
                        model: models.Question,
                        attributes: ['id', 'question', 'alternatives'],
                        as: 'question'
                    },
                    {
                        model: models.User,
                        attributes: ['id', 'name', 'email'],
                        as: 'user'
                    }
                ],
                order: [['createdAt', 'DESC']]
            });

            res.status(200).json(surveys);
        } catch (error) {
            console.error("Erro ao buscar respostas:", error);
            res.status(500).json({ error: "Falha ao buscar respostas" });
        }
    }
};