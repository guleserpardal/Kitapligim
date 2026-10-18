// Test için sahte Claude: stdin'deki istemi okur, sabit JSON yazar.
let input = '';
process.stdin.on('data', (d) => (input += d));
process.stdin.on('end', () => {
  if (input.includes('HATA_ÜRET')) { process.stderr.write('boom'); process.exit(1); }
  const flags = process.argv.slice(2).join(' ');
  console.log('```json\n' + JSON.stringify({ reply: `ok [${flags.includes('--safe-mode') ? 'safe' : 'unsafe'}]`, books: [{ title: 'Hizmetçi', author: 'Freida McFadden', why: 'Gerilim.' }] }) + '\n```');
});
