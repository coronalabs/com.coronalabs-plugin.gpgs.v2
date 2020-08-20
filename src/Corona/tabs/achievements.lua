local gpgs = require('plugin.gpgs.v2')
local json = require('json')

local isLegacy = require('settings').isLegacy
local gameNetwork
if isLegacy then
	gameNetwork = require('gameNetwork')
end

local newButton = require('classes.button').newButton

local achievementId = 'CgkIltzB658HEAIQAg'
local incrementalAchievementId = 'CgkIlLro46MXEAIQCg'
local hiddenAchievementId = 'CgkIlLro46MXEAIQCQ'

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Load achievements',
	onRelease = function()
		if not isLegacy then
			gpgs.achievements.load({
				reload = true,
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('loadAchievements', {
				listener = function(event)
					print('Load event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 2,
	label = 'Increment achievement',
	onRelease = function()
		gpgs.achievements.increment({
			achievementId = incrementalAchievementId,
			steps = 1,
			listener = function(event)
				print('Increment event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 3,
	label = 'Reveal achievement',
	onRelease = function()
		gpgs.achievements.reveal({
			achievementId = hiddenAchievementId,
			listener = function(event)
				print('Reveal event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 4,
	label = 'Set steps for achievement',
	onRelease = function()
		gpgs.achievements.setSteps({
			achievementId = incrementalAchievementId,
			steps = 5,
			listener = function(event)
				print('Set steps event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 5,
	label = 'Unlock achievement',
	onRelease = function()
		if not isLegacy then
			gpgs.achievements.unlock({
				achievementId = achievementId,
				listener = function(event)
					print('Unlock event:', json.prettify(event))
				end
			})
		else
			gameNetwork.request('unlockAchievement', {
				achievement = {
					identifier = achievementId
				},
				listener = function(event)
					print('Unlock event:', json.prettify(event))
				end
			})
		end
	end
})

newButton({
	g = group, index = 6,
	label = 'Show achievements',
	onRelease = function()
		if not isLegacy then
			gpgs.achievements.show()
		else
			gameNetwork.show('achievements')
		end
	end
})

return group
