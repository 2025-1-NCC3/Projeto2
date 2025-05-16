import models from "../models/index.js";
import bcrypt from "bcrypt";
import jwt from "jsonwebtoken";
import 'dotenv/config'

const sanitizeUser = (user) => {
    const { password, ...userData } = user.get({ plain: true });
    return userData;
};

export default {
    async signup(req, res) {
        try {
            const { name, email, phone, birthday, genero, tipoConta, cpf, cnh, salt, password } = req.body;

            const existingUser = await models.User.findOne({ where: { email } });
            if (existingUser) {
                return res.status(400).json({ error: "Email já cadastrado" });
            }

            const newUser = await models.User.create({
                name,
                email,
                phone,
                birthday,
                genero,
                tipoConta,
                cpf,
                cnh,
                salt,
                password
            });

            const token = jwt.sign(
                { id: newUser.id, email: newUser.email },
                process.env.JWT_SECRET,
                { expiresIn: "7d" }
            );

            res.status(201).json({
                user: sanitizeUser(newUser),
                token
            });

        } catch (error) {
            console.error("Erro no cadastro:", error);
            res.status(400).json({ error: "Falha no cadastro", details: error.message });
        }
    },

    async signin(req, res) {
        try {
            const { email, password } = req.body;

            const user = await models.User.findOne({ where: { email } });
            if (!user) {
                return res.status(401).json({ error: "Credenciais inválidas" });
            }

            const validPassword = await bcrypt.compare(password, user.password);
            if (!validPassword) {
                return res.status(401).json({ error: "Credenciais inválidas" });
            }

            const token = jwt.sign(
                { id: user.id, email: user.email },
                process.env.JWT_SECRET,
                { expiresIn: "7d" }
            );

            res.json({
                user: sanitizeUser(user),
                token
            });

        } catch (error) {
            console.error("Erro no login:", error);
            res.status(500).json({ error: "Falha no login" });
        }
    }
};