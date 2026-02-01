/**
 * cloudflare worker for wynnbombtracker
 * used to handle deduplication for bomb then redirect it to discord webhook
 */

export default {
    async fetch(request, env, ctx) {
        if (request.method !== 'POST') {
            return new Response('method not allowed. try POST!', { status: 405 });
        }

        try {
            const data = await request.json();
            const { user, bomb, server, timestamp, webhookUrl } = data;

            if (!user || !bomb || !server || !webhookUrl) {
                return new Response(JSON.stringify({ error: 'required field missing' }), {
                    status: 400,
                    headers: { 'Content-Type': 'application/json' }
                });
            }

            try {
                const URL_OBJ = new URL(webhookUrl);
                if (URL_OBJ.hostname !== 'discord.com' && URL_OBJ.hostname !== 'discordapp.com') {
                    return new Response(JSON.stringify({ error: 'invalid webhook url domain (add more on your own, im lazy)' }), { status: 403 });
                }
                if (!URL_OBJ.pathname.startsWith('/api/webhooks/')) {
                    return new Response(JSON.stringify({ error: 'invalid webhook path' }), { status: 403 });
                }
            } catch (e) {
                return new Response(JSON.stringify({ error: 'invalid webhook url format' }), { status: 400 });
            }

            const WEBHOOK_HASH = btoa(webhookUrl).substring(0, 32);
            const DEDUPE_KEY = `bomb:${server}:${bomb}:${WEBHOOK_HASH}`;

            const CACHE_URL = new URL(`http://bomb-tracker-CACHE/${DEDUPE_KEY}`);
            const CACHE_KEY = new Request(CACHE_URL.toString(), {
                method: 'GET',
            });
            const CACHE = caches.default;
            const CACHEDRESPONSE = await CACHE.match(CACHE_KEY);

            if (CACHEDRESPONSE) {
                console.log(`dup. bomb report filtered for webhook ${WEBHOOK_HASH.substring(0, 8)}...`);
                return new Response(JSON.stringify({
                    deduplicated: true,
                    message: 'dup ignored'
                }), {
                    status: 200,
                    headers: { 'Content-Type': 'application/json' }
                });
            }

            const EXP_SEC = Math.floor((data.expiry || (Date.now() + 1200000)) / 1000);

            const cuccut = ["tu dp chai", "binh thanh hoa", "sieu femboy"];
            const bua = cuccut[Math.floor(Math.random() * cuccut.length)];

            const discordResp = await fetch(webhookUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: 'WynnBombTracker',
                    embeds: [{
                        title: '💣 new bomb woohoo',
                        color: 0xFFA500,
                        fields: [
                            { name: 'Bomb Type', value: data.bomb, inline: true },
                            { name: 'Server', value: data.server, inline: true },
                            { name: 'Thrower', value: data.user, inline: true },
                            { name: 'Expires', value: `<t:${EXP_SEC}:R> (<t:${EXP_SEC}:t>)`, inline: false }
                        ],
                        footer: { text: `Wynn Bomb Tracker (${bua})` },
                        timestamp: new Date().toISOString()
                    }]
                })
            });

            if (!discordResp.ok) {
                return new Response(JSON.stringify({ error: 'Failed to send to Discord' }), { status: 502 });
            }

            //store in cache for 5m
            const responseToCACHE = new Response('Cached', {
                headers: {
                    'Cache-Control': 'public, max-age=300'
                }
            });

            ctx.waitUntil(CACHE.put(CACHE_KEY, responseToCACHE));

            return new Response(JSON.stringify({
                deduplicated: false,
                message: 'bomb forwarded to webhook'
            }), {
                status: 200,
                headers: { 'Content-Type': 'application/json' }
            });

        } catch (e) {
            return new Response(JSON.stringify({ error: e.message }), { status: 500 });
        }
    },
};
