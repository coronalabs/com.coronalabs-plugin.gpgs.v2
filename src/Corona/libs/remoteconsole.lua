local _M = {}

function _M.enable(hostname, port)
	if not hostname then
		error('Hostname is required', 2)
	end
	port = port or 22000
	local udp = require('socket').udp4()
	udp:setsockname('*', 0)
	udp:settimeout(100)
	rawset(_G, 'print', function(...)
		local t = {...}
		local s = ''
		for i = 1, #t do
			local v = t[i]
			if v == nil then v = 'nil' end
			s = s .. tostring(v) .. '\t'
		end
		udp:sendto(s .. '\n', hostname, port)
	end)
	Runtime:addEventListener('unhandledError', function(event)
		local message = 'Runtime error\n' .. event.errorMessage .. event.stackTrace .. '\n'
		udp:sendto(message, hostname, port)
	end)
end

return _M
