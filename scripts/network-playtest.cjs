'use strict'
// Test clients only: protocol 26.1 translated by test-only Via plugins into Paper 26.2.
// Every GUI action is sequential and checked against a server receipt.
const mineflayer = require('mineflayer')
const readline = require('readline')
const assert = require('node:assert/strict')
const { Vec3 } = require('vec3')
const sleep = ms => new Promise(r => setTimeout(r, ms))
const bots = []
const pending = new Map()
let serial = 0
function emit(value) { process.stdout.write(JSON.stringify(value) + '\n') }
readline.createInterface({ input: process.stdin }).on('line', line => {
  const value = JSON.parse(line)
  const p = pending.get(value.id)
  if (p) { pending.delete(value.id); value.error ? p.reject(Error(value.error)) : p.resolve(value) }
})
async function consoleCommand(command, expected) {
  const id = ++serial
  const result = new Promise((resolve, reject) => pending.set(id, { resolve, reject }))
  emit({ type: 'console', id, command, expected })
  return result
}
async function checkpoint(stage) {
  const id = ++serial
  const result = new Promise((resolve, reject) => pending.set(id, { resolve, reject }))
  emit({ type: 'checkpoint', id, stage })
  return result
}
async function until(predicate, description, timeout = 15000) {
  const end = Date.now() + timeout
  while (Date.now() < end) {
    if (predicate()) return
    for (const b of bots) if (b.testError) throw b.testError
    await sleep(75)
  }
  throw Error('Timeout: ' + description)
}
async function connect(username) {
  const bot = mineflayer.createBot({
    host: '127.0.0.1', port: 25576, username, auth: 'offline',
    version: '26.1', checkTimeoutInterval: 120000
  })
  bot.receipts = []
  bot.testStateId = 0
  bot._client.on('abilities', p => { bot.flightAllowed = (p.flags & 4) !== 0 })
  bot.on('messagestr', text => {
    bot.receipts.push(text)
    emit({ type: 'chat', player: username, text })
  })
  bot._client.on('window_items', p => { bot.testStateId = p.stateId ?? bot.testStateId })
  bot._client.on('set_slot', p => { bot.testStateId = p.stateId ?? bot.testStateId })
  bot.on('windowOpen', w => emit({ type: 'window', player: username, title: w.title, slots: w.inventoryStart }))
  bot.on('kicked', why => { bot.testError = Error(username + ' kicked: ' + JSON.stringify(why)) })
  bot.on('error', error => { bot.testError = error })
  bots.push(bot)
  await until(() => bot.entity, username + ' spawn', 90000)
  await sleep(500)
  return bot
}
async function chat(bot, command, expected) {
  const start = bot.receipts.length
  bot.chat(command)
  if (expected) await until(() => bot.receipts.slice(start).some(x => x.includes(expected)), command + ' receipt ' + expected)
  await sleep(350)
}
async function window(bot, size, title) {
  await until(() => bot.currentWindow && bot.currentWindow.inventoryStart === size &&
    (!title || JSON.stringify(bot.currentWindow.title).includes(title)), 'window ' + size + ' ' + title)
}
async function click(bot, slot, mode = 0) {
  assert(bot.currentWindow, 'Click requires an open custom GUI')
  const Item = require('prismarine-item')(bot.version)
  // Custom menus cancel inventory transfer. Submit the gesture without locally
  // pretending the menu's icon moved into the player's real inventory.
  bot._client.write('window_click', {
    windowId: bot.currentWindow.id, stateId: bot.testStateId, slot,
    mouseButton: 0, mode, changedSlots: [], cursorItem: Item.toNotch(null)
  })
  await sleep(450)
}
async function close(bot) {
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow)
  await sleep(200)
}
async function wave(bot) {
  await close(bot)
  bot.setQuickBarSlot(2)
  await sleep(150)
  await consoleCommand('execute as ' + bot.username +
    ' if items entity @s weapon.mainhand minecraft:chest run say CTD_SUMMONER_HELD_' + bot.username,
    'CTD_SUMMONER_HELD_' + bot.username)
  emit({ type: 'client-held-replica', player: bot.username, slot: bot.quickBarSlot,
    item: bot.heldItem?.name ?? null })
  await bot.lookAt(bot.entity.position.offset(0, 4, 0), true)
  // Use the pinned client's public API so block/use gestures share its sequence.
  bot.activateItem()
  await window(bot, 27)
  const start = bot.receipts.length
  await click(bot, 0, 1)
  await until(() => bot.receipts.slice(start).some(x => (x.includes('DraftChanged') || x.includes('已选怪物'))), 'summoner draft')
  const sendStart = bot.receipts.length
  await click(bot, 26)
  await until(() => bot.receipts.slice(sendStart).some(x => (x.includes('BatchSent') || x.includes('已派出'))), 'paid batch send')
  await close(bot)
}
function count(bot, name) { return bot.inventory.items().filter(x => x.name === name).reduce((n, x) => n + x.count, 0) }
async function restored(red, blue) {
  await until(() => count(red, 'diamond') === 7 && count(blue, 'emerald') === 9 &&
    red.experience.level === 13 && red.game.gameMode === 'creative' &&
    blue.game.gameMode === 'survival' && red.flightAllowed === true && blue.flightAllowed === false && red.entity.position.distanceTo(new Vec3(0.5, 100, 0.5)) < 1 &&
    blue.entity.position.distanceTo(new Vec3(1.5, 100, 0.5)) < 1,
  'exact hub items / XP / modes / positions restored', 30000)
}
async function tower(red) {
  await consoleCommand('execute in minecraft:ctd_farm run tp TD_Red 20.5 89 76.5')
  await until(() => red.entity.position.distanceTo(new Vec3(20.5, 89, 76.5)) < 2, 'plot teleport')
  await sleep(1200)
  red.setQuickBarSlot(0)
  const plot = red.blockAt(new Vec3(20, 88, 79))
  assert(plot && plot.name !== 'air', 'Farm plot must be loaded')
  await red.activateBlock(plot)
  await window(red, 45)
  const floor = []
  for (let x = 19; x <= 21; x++) for (let z = 78; z <= 80; z++) {
    const p = new Vec3(x, 88, z)
    floor.push([p, red.blockAt(p).name])
  }
  function checkFoundation() {
    for (const [p, name] of floor) assert.equal(red.blockAt(p).name, name, 'Tower must preserve floor')
    assert.notEqual(red.blockAt(new Vec3(19, 89, 78)).name, 'air', 'Foundation must start above floor')
  }
  await click(red, 2)
  await window(red, 27, '选择升级路线')
  await click(red, 11)
  await until(() => !red.currentWindow, 'path selection places tower')
  await sleep(600)
  checkFoundation()
  async function openManagement() {
    red.setQuickBarSlot(0)
    const body = red.blockAt(new Vec3(20, 89, 79))
    assert(body && body.name !== 'air', 'Archer model must exist')
    await red.activateBlock(body)
    await window(red, 54, '弓箭塔')
  }
  await openManagement()
  await click(red, 13)
  await window(red, 54, '弓箭塔 II')
  await click(red, 45)
  await until(() => JSON.stringify(red.currentWindow?.slots[45]).includes('关闭'), 'rangefinder Disable feedback')
  await click(red, 13)
  await window(red, 54, '弓箭塔 III')
  checkFoundation()
  await close(red)
  emit({ type: 'assertion', check: 'builder_path_model_upgrade_rangefinder', status: 'PASS' })
}
async function play(red, blue, round) {
  await consoleCommand('tick rate 20')
  await chat(red, '/ctdjoin')
  await chat(red, '/ctdvote pricing quick')
  await chat(red, '/ctdvote armageddon horde')
  await chat(blue, '/ctdjoin')
  await until(() => red.game.gameMode === 'adventure' && blue.game.gameMode === 'adventure' &&
    red.entity.position.y > 80 && blue.entity.position.y > 80, 'queued match bootstrap', 45000)
  assert.equal(red.flightAllowed, true, 'Red may fly in Adventure mode')
  assert.equal(blue.flightAllowed, true, 'Survival player may fly during match')
  await checkpoint('active')
  const endStart = red.receipts.length
  await wave(blue)
  await tower(red)
  await wave(red)
  await consoleCommand('execute in minecraft:ctd_farm if loaded 14 88 160 run say CTD_REMOTE_ROUTE_CHUNK_LOADED', 'CTD_REMOTE_ROUTE_CHUNK_LOADED')
  // Only this isolated scratch test server accelerates its game clock.
  await consoleCommand('tick rate 100')
  const start = Date.now()
  let batches = 2
  while (!red.receipts.slice(endStart).some(x => (x.includes('Top players') || x.includes('本局最佳玩家')))) {
    if (Date.now() - start > 420000) throw Error('No natural match outcome within seven minutes')
    if (red.receipts.slice(endStart).some(x => /runtime|ERROR|stopped/i.test(x) && /failed|error|stopped/i.test(x))) {
      throw Error('Unexpected protective match end: ' + red.receipts.slice(endStart).join('\n'))
    }
    await sleep(3500)
    if (red.receipts.slice(endStart).some(x => (x.includes('Top players') || x.includes('本局最佳玩家')))) break
    try { await wave(red) } catch (error) {
      if (red.receipts.slice(endStart).some(x => (x.includes('Top players') || x.includes('本局最佳玩家')))) break
      throw error
    }
    batches++
    if (Date.now() - start > 25000) await checkpoint('active')
  }
  await consoleCommand('tick rate 20')
  await close(red); await close(blue)
  await restored(red, blue)
  await checkpoint('restored')
  await consoleCommand('ctdpreflight', 'Composition preflight PASS:')
  emit({ type: 'round', round, status: 'PASS', naturalOutcome: true, paidBatches: batches, elapsedMs: Date.now() - start })
}
async function main() {
  const red = await connect('TD_Red')
  const blue = await connect('TD_Blue')
  await consoleCommand('op TD_Red')
  if (process.env.CTD_TEST_PHASE === 'recovery') {
    await chat(red, '/ctdsnapshotcheck restart-status', 'passed=true')
    await restored(red, blue)
    await checkpoint('restart_restored')
    emit({ type: 'complete', phase: 'recovery' })
  } else {
    await consoleCommand('fill -4 99 -4 4 99 4 minecraft:stone')
    for (const [bot, x, item, amount] of [[red, 0.5, 'diamond', 7], [blue, 1.5, 'emerald', 9]]) {
      await consoleCommand('gamemode ' + (bot === blue ? 'survival ' : 'creative ') + bot.username)
      await consoleCommand('execute in minecraft:overworld run tp ' + bot.username + ' ' + x + ' 100 0.5')
      await consoleCommand('clear ' + bot.username)
      await consoleCommand('give ' + bot.username + ' minecraft:' + item + ' ' + amount)
      await consoleCommand('experience set ' + bot.username + ' 13 levels')
      await consoleCommand('effect give ' + bot.username + ' minecraft:instant_health 1 10')
    }
    await restored(red, blue)
    await chat(red, '/ctdsnapshotcheck', '玩家状态检查通过')
    await play(red, blue, 1)
    await play(red, blue, 2)
    // External hub/world plugins must not strand an active participant with a
    // cleared inventory or let same-coordinate gestures operate on the arena.
    await chat(red, '/ctdjoin')
    await chat(red, '/ctdvote pricing quick')
    await chat(blue, '/ctdjoin')
    await until(() => red.game.gameMode === 'adventure' && blue.game.gameMode === 'adventure',
      'cross-world recovery match starts', 45000)
    await checkpoint('active')
    await tower(red)
    await wave(blue)
    for (const bot of [red, blue]) {
      await consoleCommand('execute in minecraft:overworld run tp ' + bot.username + ' 10.5 100 10.5')
      await until(() => bot.game.gameMode === (bot === blue ? 'survival' : 'creative'), 'external world teleport restores ' + bot.username)
    }
    await restored(red, blue)
    await checkpoint('restored')
    await consoleCommand('ctdpreflight', 'Composition preflight PASS:')
    emit({ type: 'assertion', check: 'external_world_departure_restores_and_cleans_arena', status: 'PASS' })
    await chat(red, '/ctdsnapshotcheck restart-arm', 'probe ARMED')
    await sleep(2200) // Same-process recovery sweep must leave the held probe intact.
    await checkpoint('restart_armed')
    emit({ type: 'complete', phase: 'matches' })
  }
  for (const bot of bots) bot.quit()
  await sleep(500)
  process.exit(0)
}
main().catch(error => {
  emit({ type: 'failure', error: error.stack })
  for (const bot of bots) bot.quit()
  setTimeout(() => process.exit(1), 200)
})
