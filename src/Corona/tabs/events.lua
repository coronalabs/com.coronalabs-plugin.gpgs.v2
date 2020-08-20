local gpgs = require('plugin.gpgs.v2')
local json = require('json')

local newButton = require('classes.button').newButton

local eventId = 'CgkIoLmv74geEAIQAw'

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Load events',
	onRelease = function()
		gpgs.events.load({
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 2,
	label = 'Load event',
	onRelease = function()
		gpgs.events.load({
			eventId = eventId,
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 3,
	label = 'Load 2 events',
	onRelease = function()
		gpgs.events.load({
			eventIds = {'CgkIoLmv74geEAIQAw', 'CgkIoLmv74geEAIQBA'},
			reload = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 4,
	label = 'Increment event',
	onRelease = function()
		gpgs.events.increment({
			eventId = eventId,
			value = 1,
			listener = function(event)
				print('Increment event:', json.prettify(event))
			end
		})
	end
})

return group
