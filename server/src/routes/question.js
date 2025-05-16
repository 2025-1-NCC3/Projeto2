import { Router } from 'express';
import { getAll } from '../controllers/questionController.js';
import authMiddleware from '../middlewares/auth.js';

const router = Router();
router.use(authMiddleware);
router.get('/', getAll);

export default router;