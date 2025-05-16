import { Router } from 'express';
import userController from '../controllers/userController.js';
import authMiddleware from '../middlewares/auth.js';

const router = Router();
router.use(authMiddleware);
router.route('/')
    .get(userController.getCurrentUser)
    .put(userController.updateUser)
    .post(userController.updateUser);

export default router;