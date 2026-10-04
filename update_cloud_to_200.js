const { CloudSyncService } = require('./web/src/cloudSync.js');

async function updateCloudTo200() {
  const cs = new CloudSyncService();
  console.log('1. Fetching current cloud data...');
  const cloud = await cs.fetchCloudData(true);

  if (!cloud.updates) {
    cloud.updates = {};
  }

  cloud.updates.athleteVersion = '2.0.0';
  cloud.updates.athleteUrl = 'https://fitness-ecosystem-pro.onrender.com/releases/athlete-pro-v2.0.0.apk';
  cloud.updates.trainerVersion = '2.0.0';
  cloud.updates.trainerUrl = 'https://fitness-ecosystem-pro.onrender.com/releases/trainer-pro-v2.0.0.apk';
  cloud.updates.notes = 'Версия 2.0.0: Pull-to-Refresh обновление как в браузере (Профиль/Настройки), 1-клик Telegram вход, 2FA OTP защита, синхронизация Google Drive.';
  cloud.updatedAt = String(Date.now());

  console.log('2. Pushing updated version 2.0.0 to cloud node...');
  const pushed = await cs.pushCloudData(cloud);
  console.log('Push result:', pushed);

  console.log('3. Verifying updated cloud node...');
  const verified = await cs.fetchCloudData(true);
  console.log('Verified Athlete Version in Cloud:', verified?.updates?.athleteVersion);
  console.log('Verified Trainer Version in Cloud:', verified?.updates?.trainerVersion);
}

updateCloudTo200().catch(console.error);
