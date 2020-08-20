local gpgs = require('plugin.gpgs.v2')
local json = require('json')
local widget = require('widget')

local newButton = require('classes.button').newButton

local serverId = '799878552852-oov42vbfupkgi3a8263ge9qvrkrgmjqh.apps.googleusercontent.com'

local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'App id',
	onRelease = function()
		native.showAlert('App id', gpgs.getAppId(), {'OK'})
	end
})

newButton({
	g = group, index = 2,
	label = 'Account name',
	onRelease = function()
		gpgs.getAccountName(function(event)
			print('accountName event:', json.prettify(event))
		end)
	end
})

newButton({
	g = group, index = 3,
	label = 'Server auth code',
	onRelease = function()
		gpgs.getServerAuthCode({
			serverId = serverId,
			listener = function(event)
				print('serverAuthCode event:', json.prettify(event))
			end
		})
	end
})

newButton({
	g = group, index = 4,
	label = 'SDK version',
	onRelease = function()
		native.showAlert('SDK version', gpgs.getSdkVersion(), {'OK'})
	end
})

newButton({
	g = group, index = 5,
	label = 'Popups to bottom',
	onRelease = function()
		gpgs.setPopupPosition('BottomCenter')
	end
})

newButton({
	g = group, index = 6,
	label = 'Show settings',
	onRelease = function()
		gpgs.showSettings(function(event)
			print('showSettings event:', json.prettify(event))
		end)
	end
})

newButton({
	g = group, index = 7,
	label = 'Load game',
	onRelease = function()
		gpgs.loadGame(function(event)
			print('loadGame event:', json.prettify(event))
		end)
	end
})

newButton({
	g = group, index = 8,
	label = 'Clear notifications',
	onRelease = function()
		gpgs.clearNotifications()
	end
})

newButton({
	g = group, index = 9,
	label = 'Load image',
	onRelease = function()
		gpgs.players.load({listener = function(event)
			if not event.isError and event.players[1].largeImageUri then
				local uri = event.players[1].largeImageUri
				gpgs.loadImage({
					uri = uri,
					filename = 'test.png',
					baseDir = system.CachesDirectory,
					listener = function(event)
						print('loadImage event:', json.prettify(event))
						local image = display.newImage(group, event.filename, event.baseDir, display.contentCenterX, display.contentCenterY)
						image:addEventListener('tap', function(tapEvent)
							tapEvent.target:removeSelf()
						end)
					end
				})
			end
		end})
	end
})

local scrollView = widget.newScrollView({
	left = display.screenOriginX, top = display.screenOriginY,
	width = display.actualContentWidth, height = display.actualContentHeight - require('settings').tabBarHeight,
	horizontalScrollDisabled = true
})

scrollView:insert(group)

return scrollView
