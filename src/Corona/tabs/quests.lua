local gpgs = require('plugin.gpgs.v2')
local json = require('json')

local newButton = require('classes.button').newButton

local questId = 'CgkIlLro46MXEAIQEQ'
local milestoneId = '<ChwKCQjYqKqLyQMQARINCgkIlLro46MXEAIQDxgAEgIIAQ'

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Load quests',
	onRelease = function()
		gpgs.quests.load({
			recentlyUpdatedFirst = true,
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 2,
	label = 'Load quest',
	onRelease = function()
		gpgs.quests.load({
			questId = questId,
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 3,
	label = 'Load 2 quests',
	onRelease = function()
		gpgs.quests.load({
			questIds = {'<CgkI2Kiqi8kDEAESDQoJCJS66OOjFxACEA4YAA', '<CgkI2Kiqi8kDEAESDQoJCJS66OOjFxACEA8YAA'},
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 4,
	label = 'Accept quest',
	onRelease = function()
		gpgs.quests.accept({
			questId = questId,
			listener = function(event)
				print('Accept event:', json.prettify(event))
				milestoneId = event.quest.milestone.id
			end
		})
	end
})

newButton({
	g = group, index = 5,
	label = 'Claim quest',
	onRelease = function()
		gpgs.quests.claim({
			questId = questId,
			milestoneId = milestoneId,
			listener = function(event)
				print('Claim event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 6,
	label = 'Show quests',
	onRelease = function()
		gpgs.quests.show({
			questId = questId,
			listener = function(event)
				print('Show event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 7,
	label = 'Show popup',
	onRelease = function()
		gpgs.quests.showPopup(questId)
	end
})

newButton({
	g = group, index = 8,
	label = 'Set Listener',
	onRelease = function()
		gpgs.quests.setListener(function(event)
			print('Quest event:', json.prettify(event))
		end)
	end
})

return group
