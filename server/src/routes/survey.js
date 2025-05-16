import { Router } from 'express';
import { create } from '../controllers/surveyController.js';
import authMiddleware from '../middlewares/auth.js';

const router = Router();
router.use(authMiddleware);
router.post('/', create);
export default router;