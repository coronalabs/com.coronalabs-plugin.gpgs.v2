display.setStatusBar(display.HiddenStatusBar)
display.setDefault('background', 1)

local widget = require('widget')

io.output():setvbuf('no')
--require('libs.remoteconsole').enable('192.168.0.108', system.getInfo('model') == 'Moto G' and 21000 or 22000)

math.randomseed(os.time())

local h = 24
local settings = {
	isLegacy = false, -- If true, use legacy gameNetwork.* API when available
	tabBarHeight = h * 3
}
-- Set up custom virtual Lua module
package.loaded.settings = settings

local tabs = {}
tabs.auth = require('tabs.auth')
tabs.achievements = require('tabs.achievements')
tabs.leaderboards = require('tabs.leaderboards')
tabs.extra = require('tabs.extra')
tabs.events = require('tabs.events')
tabs.players = require('tabs.players')
tabs.quests = require('tabs.quests')
tabs.snapshots = require('tabs.snapshots')
tabs.multiplayer = require('tabs.multiplayer')
tabs.videos = require('tabs.videos')

-- Hide all tab groups except one
local function showTab(name)
	for k, v in pairs(tabs) do
		v.isVisible = k == name
	end
end

showTab('auth')

-- Configure the tab buttons to appear within the tabbar
local tabButtons = {
	{
		{label = 'Auth', onPress = function() showTab('auth') end, selected = true},
		{label = 'Achievements', onPress = function() showTab('achievements') end},
		{label = 'Leaderboards', onPress = function() showTab('leaderboards') end},
		{label = 'Extra', onPress = function() showTab('extra') end}
	},{
		{label = 'Events', onPress = function() showTab('events') end},
		{label = 'Players', onPress = function() showTab('players') end},
		{label = 'Quests', onPress = function() showTab('quests') end},
		{label = 'Requests', onPress = function() showTab('requests') end}
	},{
		{label = 'Snapshots', onPress = function() showTab('snapshots') end},
		{label = 'Multiplayer', onPress = function() showTab('multiplayer') end},
		{label = 'Videos', onPress = function() showTab('videos') end}
	}
}

for i = 1, #tabButtons do
	widget.newTabBar({
		left = display.screenOriginX, top = display.screenOriginY + display.actualContentHeight - h * (#tabButtons - i + 1),
		width = display.actualContentWidth, height = h,
		buttons = tabButtons[i]
	})
end
