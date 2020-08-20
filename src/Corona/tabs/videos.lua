local gpgs = require('plugin.gpgs.v2')
local json = require('json')

local newButton = require('classes.button').newButton

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Is supported?',
	onRelease = function()
		native.showAlert('Is supported?', gpgs.videos.isSupported() and 'Yes' or 'No', {'OK'})
	end
})

newButton({
	g = group, index = 2,
	label = 'Load сapabilities',
	onRelease = function()
		gpgs.videos.loadCapabilities(function(event)
			print('Load event:', json.prettify(event))
		end)
	end
})

newButton({
	g = group, index = 3,
	label = 'Is file mode available',
	onRelease = function()
		gpgs.videos.isModeAvailable({
			mode = 'file',
			listener = function(event)
				print('isModeAvailable event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 4,
	label = 'Is stream mode available',
	onRelease = function()
		gpgs.videos.isModeAvailable({
			mode = 'stream',
			listener = function(event)
				print('isModeAvailable event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 5,
	label = 'Get state',
	onRelease = function()
		gpgs.videos.getState(function(event)
			print('getState event:', json.prettify(event))
		end)
	end
})

newButton({
	g = group, index = 6,
	label = 'Show overlay',
	onRelease = function()
		gpgs.videos.show(function(event)
			print('Show event:', json.prettify(event))
		end)
	end
})

newButton({
	g = group, index = 7,
	label = 'Set listener',
	onRelease = function()
		gpgs.videos.setListener(function(event)
			print('Video event:', json.prettify(event))
		end)
	end
})

return group
