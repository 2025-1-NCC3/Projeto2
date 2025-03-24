import { Router } from 'express';
import questionController from '../controllers/questionController.js';

const router = Router();

router.get("/questions", questionController.getAll);
router.get("/questions/:id", questionController.getById);

export default router;