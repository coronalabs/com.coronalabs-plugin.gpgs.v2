local newButton = require('classes.button').newButton

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Launch Realtime',
	onRelease = function()
		require('multiplayer.realtime'):show()
	end
})

newButton({
	g = group, index = 2,
	label = 'Launch Turnbased',
	onRelease = function()
		require('multiplayer.turnbased'):show()
	end
})

return group
