local gpgs = require('plugin.gpgs.v2')
local json = require('json')

local isLegacy = require('settings').isLegacy
local gameNetwork
if isLegacy then
	gameNetwork = require('gameNetwork')
end

local newButton = require('classes.button').newButton

local leaderboardId = 'CgkIlLro46MXEAIQBg'

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Load leaderboards',
	onRelease = function()
		if not isLegacy then
			gpgs.leaderboards.load({
				reload = true,
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('loadLeaderboardCategories', {
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 2,
	label = 'Load scores',
	onRelease = function()
		if not isLegacy then
			gpgs.leaderboards.loadScores({
				leaderboardId = leaderboardId,
				reload = true,
				listener = function(event)
					print('Load scores event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('loadScores', {
				leaderboard = {
					category = leaderboardId
				},
				listener = function(event)
					print('Load scores event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 3,
	label = 'Load player score',
	onRelease = function()
		if not isLegacy then
			gpgs.leaderboards.loadScores({
				leaderboardId = leaderboardId,
				position = 'single',
				reload = true,
				listener = function(event)
					print('Load scores event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('loadScores', {
				leaderboard = {
					category = leaderboardId,
					range = {1, 1},
					playerCentered = true
				},
				listener = function(event)
					print('Load scores event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 4,
	label = 'Load weekly scores',
	onRelease = function()
		if not isLegacy then
			gpgs.leaderboards.loadScores({
				leaderboardId = leaderboardId,
				timeSpan = 'weekly',
				position = 'centered',
				reload = true,
				listener = function(event)
					print('Load scores event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('loadScores', {
				leaderboard = {
					category = leaderboardId,
					timeScope = 'Week',
					playerCentered = true
				},
				listener = function(event)
					print('Load scores event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 5,
	label = 'Submit score',
	onRelease = function()
		if not isLegacy then
			gpgs.leaderboards.submit({
				leaderboardId = leaderboardId,
				score = 1100,
				tag = 'testtag',
				listener = function(event)
					print('Submit event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('setHighScore', {
				localPlayerScore = {
					category = leaderboardId,
					value = 1200
				},
				listener = function(event)
					print('Set high score event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 6,
	label = 'Show leaderboards',
	onRelease = function()
		if not isLegacy then
			gpgs.leaderboards.show()
		else
			gameNetwork.show('leaderboards')
		end
	end
})

newButton({
	g = group, index = 7,
	label = 'Show leaderboard',
	onRelease = function()
		gpgs.leaderboards.show({
			leaderboardId = leaderboardId,
			timeSpan = 'daily'
		})
	end
})

return group
