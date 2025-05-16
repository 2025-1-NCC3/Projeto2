import 'dotenv/config';
import User from '../models/User.js';

const sanitizeUser = (user) => {
    const { password, ...userData } = user.get({ plain: true });
    return userData;
};

export default {
    async getCurrentUser(req, res) {
        try {
            const userId = req.user.id;
            const user = await User.findByPk(userId);

            if (!user) {
                return res.status(404).json({ error: 'Usuário não encontrado' });
            }

            const userData = sanitizeUser(user);
            res.json(userData);

        } catch (error) {
            res.status(500).json({ error: error.message });
        }
    },

    async updateUser(req, res) {
        try {
            const userId = req.user.id;
            const user = await User.findByPk(userId);

            if (!user) {
                return res.status(404).json({ error: 'Usuário não encontrado' });
            }

            // Atualiza apenas os campos enviados no body
            const updatable = ['name', 'surname', 'email', 'phone', 'birthday', 'genero', 'tipoConta', 'cpf', 'cnh', 'carro'];
            updatable.forEach(f => {
                if (req.body[f] !== undefined) user[f] = req.body[f];
            });

            if (req.body.senha) {
                user.password = req.body.senha;
                user.salt = req.body.salt;
            }

            await user.save();

            const userData = sanitizeUser(user);
            res.status(201).json(userData);

        } catch (error) {
            // Tratamento de erros do Sequelize
            if (error.name === 'SequelizeUniqueConstraintError') {
                return res.status(400).json({ error: 'Email já está em uso' });
            }

            if (error.name === 'SequelizeValidationError') {
                const messages = error.errors.map(err => err.message);
                return res.status(400).json({ errors: messages });
            }

            res.status(500).json({ error: 'Erro ao atualizar usuário' });
        }
    },

    getAllUsers: async (req, res) => {
        try {
            const users = await User.findAll();
            res.json(users);
        } catch (error) {
            res.status(500).json({ error: 'Erro ao buscar usuários' });
        }
    },

    async registerCar(req, res) {
        try {
            const { brand, model, plate, color, year } = req.body;
            const user = await User.findByPk(req.userId);
            user.carro = { brand, model, plate, color, year };
            await user.save();
            res.status(201).json({ success: true });
        } catch (err) {
            res.status(400).json({ error: err.message });
        }
    }
};