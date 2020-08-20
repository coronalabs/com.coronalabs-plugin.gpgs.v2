local json = require('json')
local isLegacy = require('settings').isLegacy
local gameNetwork
local newButton = require('classes.button').newButton

local gpgs = require('plugin.gpgs.v2')
gpgs.enableDebug()

if isLegacy then
	gameNetwork = require('gameNetwork')
end

local function loginListener(event)
	print('Login event:', json.prettify(event))
end

if not isLegacy then
	-- Init Google Play Game Services
	if gpgs.init then
		gpgs.init(function(event)
			print('Init event:', json.prettify(event))
			if not event.isError then
				-- Try to automatically log in the user without displaying the login screen if the user doesn't want to login
				gpgs.login({
					listener = loginListener
				})
			end
		end)
	end
else
	gameNetwork.init('google', function(event)
		print('Init event:', json.prettify(event))
		if not event.isError then
			gameNetwork.request('login', {
				listener = loginListener
			})
		end
	end)
end


local group = display.newGroup()

newButton({
	g = group, index = 1,
	label = 'Login',
	onRelease = function()
		if not isLegacy then
			gpgs.login({
				userInitiated = true,
				listener = loginListener
			})
		else
			gameNetwork.request('login', {
				userInitiated = true,
				listener = loginListener
			})
		end
	end
})

newButton({
	g = group, index = 2,
	label = 'Logout',
	onRelease = function()
		if not isLegacy then
			gpgs.logout()
		else
			gameNetwork.request('logout')
		end
	end
})

newButton({
	g = group, index = 3,
	label = 'Is connected?',
	onRelease = function()
		local result
		if not isLegacy then
			result = gpgs.isConnected()
		else
			result = gameNetwork.request('isConnected')
		end
		native.showAlert('Is connected?', result and 'Yes' or 'No', {'OK'})
	end
})

return group
