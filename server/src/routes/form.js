import formController from './controllers/formController.js';

router.put(
    '/formulario',
    authMiddleware,
    formController.updateTemperament
);