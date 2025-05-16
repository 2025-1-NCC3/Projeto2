import { Router } from 'express';
import carController from '../controllers/userController.js';
import authMiddleware from '../middleware/auth.js';

const router = Router();
router.use(authMiddleware);
router.put('/', carController.registerCar);
export default router;