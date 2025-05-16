import jwt from 'jsonwebtoken';
import models from '../models/index.js';

export default async function authMiddleware(req, res, next) {
    try {
        const authHeader = req.headers.authorization;
        
        if (!authHeader) {
            return res.status(401).json({ error: 'Token não fornecido' });
        }

        const [, token] = authHeader.split(' ');

        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        const user = await models.User.findByPk(decoded.id);

        if (!user) {
            return res.status(401).json({ error: 'Usuário não encontrado' });
        }

        req.user = user;
        next();
        
    } catch (error) {
        console.error('Erro na autenticação:', error);
        
        if (error.name === 'JsonWebTokenError') {
            return res.status(401).json({ error: 'Token inválido' });
        }
        
        if (error.name === 'TokenExpiredError') {
            return res.status(401).json({ error: 'Token expirado' });
        }
        
        res.status(500).json({ error: 'Erro na autenticação' });
    }
};