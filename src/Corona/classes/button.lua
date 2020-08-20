local _M = {}

local widget = require('widget')

local _W, _H = display.actualContentWidth, display.actualContentHeight
local _CX = display.contentCenterX

local width = _W * 0.8
local size = _H * 0.1
local buttonFontSize = size / 4

local y = display.screenOriginY
local spacing = _H * 0.1

function _M.newButton(params)
	local button = widget.newButton{
		x = _CX, y = y + spacing * params.index,
		width = width, height = size,
		label = params.label,
		fontSize = buttonFontSize,
		onRelease = function(event)
			params.onRelease(event)
		end
	}
	params.g:insert(button)
	return button
end

return _M
