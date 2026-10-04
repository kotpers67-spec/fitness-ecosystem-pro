const { DatabaseSync } = require('node:sqlite');
const path = require('path');

const dbPath = path.join(__dirname, '..', 'fitness.sqlite');
const db = new DatabaseSync(dbPath);

console.log('Before cleanup users count:', db.prepare('SELECT COUNT(*) as count FROM users').get().count);

db.exec(`
  PRAGMA foreign_keys = OFF;

  DELETE FROM workout_sets 
  WHERE session_id IN (
    SELECT id FROM workout_sessions 
    WHERE athlete_id IN (
      SELECT id FROM users 
      WHERE full_name LIKE '%Смирнов%' 
         OR full_name LIKE '%Smirnov%'
         OR full_name LIKE '%Тест%' 
         OR full_name LIKE '%Спам%' 
         OR username LIKE 'ath_%' 
         OR username LIKE 'pin_%' 
         OR username LIKE 'spam_%' 
         OR username LIKE 'tamper_%' 
         OR username LIKE 'xss_%'
         OR username LIKE 'test_%'
         OR username LIKE 'adv_%'
         OR username LIKE 'logout_%'
         OR username LIKE 'phone_test_%'
         OR username LIKE 'trn_%'
    )
  );

  DELETE FROM workout_sessions 
  WHERE athlete_id IN (
    SELECT id FROM users 
    WHERE full_name LIKE '%Смирнов%' 
       OR full_name LIKE '%Smirnov%'
       OR full_name LIKE '%Тест%' 
       OR full_name LIKE '%Спам%' 
       OR username LIKE 'ath_%' 
       OR username LIKE 'pin_%' 
       OR username LIKE 'spam_%' 
       OR username LIKE 'tamper_%' 
       OR username LIKE 'xss_%'
       OR username LIKE 'test_%'
       OR username LIKE 'adv_%'
       OR username LIKE 'logout_%'
       OR username LIKE 'phone_test_%'
       OR username LIKE 'trn_%'
  );

  DELETE FROM trainer_clients;
  DELETE FROM anthropometry;
  DELETE FROM auth_tokens;

  DELETE FROM users 
  WHERE full_name LIKE '%Смирнов%' 
     OR full_name LIKE '%Smirnov%'
     OR full_name LIKE '%Тест%' 
     OR full_name LIKE '%Спам%' 
     OR username LIKE 'ath_%' 
     OR username LIKE 'pin_%' 
     OR username LIKE 'spam_%' 
     OR username LIKE 'tamper_%' 
     OR username LIKE 'xss_%'
     OR username LIKE 'test_%'
     OR username LIKE 'adv_%'
     OR username LIKE 'logout_%'
     OR username LIKE 'phone_test_%'
     OR username LIKE 'trn_%';

  PRAGMA foreign_keys = ON;
`);

console.log('After cleanup users:');
console.log(db.prepare('SELECT id, username, full_name, role FROM users').all());
db.close();
