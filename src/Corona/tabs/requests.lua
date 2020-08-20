local gpgs = require('plugin.gpgs.v2')
local json = require('json')

local newButton = require('classes.button').newButton

local requestId = ''

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Load requests',
	onRelease = function()
		gpgs.requests.load({
			--outcoming = true,
			listener = function(event)
				print('Load event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 2,
	label = 'Accept requests',
	onRelease = function()
		gpgs.requests.accept({
			requestId = requestId,
			listener = function(event)
				print('Accept event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 3,
	label = 'Dismiss requests',
	onRelease = function()
		gpgs.requests.dismiss({
			requestId = requestId,
			listener = function(event)
				print('Dismiss event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 4,
	label = 'Get limits',
	onRelease = function()
		print(json.prettify(gpgs.requests.getLimits()))
	end
})

newButton({
	g = group, index = 5,
	label = 'Show send',
	onRelease = function()
		gpgs.requests.showSend({
			type = 'gift',
			payload = 'giftpayload',
			description = 'gift description',
			image = {
				filename = 'images/gift.png',
				baseDir = system.ResourceDirectory
			},
			listener = function(event)
				print('Send event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 6,
	label = 'Show',
	onRelease = function()
		gpgs.requests.show(function(event)
			print('Show event:', json.prettify(event))
		end)
	end
})

newButton({
	g = group, index = 7,
	label = 'Set listener',
	onRelease = function()
		gpgs.requests.setListener(function(event)
			print('Request event:', json.prettify(event))
		end)
	end
})

return group
