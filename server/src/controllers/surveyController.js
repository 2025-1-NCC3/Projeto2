import Survey from '../models/Survey.js';

const VALID_IDS = Array.from({ length: 15 }, (_, i) => i + 1);

export async function create(req, res) {
  try {
    const entries = Array.isArray(req.body) ? req.body : [req.body];
    // validação rápida
    entries.forEach(e => {
      if (!VALID_IDS.includes(e.questionId) || !['sim','não'].includes(e.answer)) {
        throw new Error(`Pergunta ou resposta inválida: ${JSON.stringify(e)}`);
      }
    });
    const surveys = await Promise.all(entries.map(e =>
      Survey.create({
        userId:     req.userId,
        questionId: e.questionId,
        answer:     e.answer
      })
    ));
    res.status(201).json(surveys);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
}
