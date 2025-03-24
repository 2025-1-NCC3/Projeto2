import { Router } from 'express';
import surveyController from '../controllers/surveyController.js';

const router = Router();

router.post("/surveys", surveyController.create);
router.get("/surveys", surveyController.getAllByUser);

export default router;