local gpgs = require('plugin.gpgs.v2')
local json = require('json')

local isLegacy = require('settings').isLegacy
local gameNetwork
if isLegacy then
	gameNetwork = require('gameNetwork')
end

local newButton = require('classes.button').newButton

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Load current player',
	onRelease = function()
		if not isLegacy then
			gpgs.players.load({
				reload = true,
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('loadlLocalPlayer', {
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 2,
	label = 'Load players',
	onRelease = function()
		if not isLegacy then
			gpgs.players.load({
				playerIds = {
					'g10870401843522296197',
					'g12701443010972034667',
					'g05466784403268559785',
					'g10654019865429722858',
					'g09477680106141148263'
				},
				reload = true,
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('loadPlayers', {
				playerIDs = {
					'g10870401843522296197',
					'g12701443010972034667',
					'g05466784403268559785',
					'g10654019865429722858',
					'g09477680106141148263'
				},
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 3,
	label = 'Load invitable/friends',
	onRelease = function()
		if not isLegacy then
			gpgs.players.load({
				source = 'invitable',
				reload = true,
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('loadFriends', {
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 4,
	label = 'Load connected players',
	onRelease = function()
		gpgs.players.load({
			source = 'connected',
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 5,
	label = 'Load recently played with',
	onRelease = function()
		gpgs.players.load({
			source = 'recentlyPlayedWith',
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 6,
	label = 'Load stats',
	onRelease = function()
		gpgs.players.loadStats({
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 7,
	label = 'Show compare',
	onRelease = function()
		gpgs.players.showCompare({
			playerId = 'g10870401843522296197',
			listener = function(event)
				print('Compare event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 8,
	label = 'Show search',
	onRelease = function()
		gpgs.players.showSearch(function(event)
			print('Search event:', json.prettify(event))
		end)
	end
})

return group
