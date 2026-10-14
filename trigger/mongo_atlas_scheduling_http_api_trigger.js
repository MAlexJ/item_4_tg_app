function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

exports = async function(arg) {
    const url = "SITE_URL/actuator/health";
    const triggerName = "Scheduling every 1 minute";
    const now = new Date();
    const FOUR_HOURS_MS = 4 * 60 * 60 * 1000;

    const db = context.services.get("Cluster0").db("notification-telegram-standalone-db");
    const state = db.collection("trigger_lock_state");
    const errors = db.collection("trigger_error");
    const successes = db.collection("trigger_success");

    // Used only for success logging
    const lockKey = "sync_lock";

    // Add a random delay between 1 sec and 10 sec
    const randomDelayMs = Math.floor(Math.random() * (10000 - 1000 + 1)) + 1000;
    await delay(randomDelayMs);

    try {
        const response = await context.http.get({ url });
        const status = response.status;
        const statusCode = response.statusCode;

        if (statusCode >= 400) {
            // Explicitly log HTTP errors
            await errors.insertOne({
                trigger: triggerName,
                function: "health_api_check",
                api: "Server API",
                status: statusCode,
                body: status,
                timestamp: now
            });

            return status;
        }

        // Check last success lock time
        const lock = await state.findOne({ _id: lockKey });
        const lastLogTime = lock?.timestamp || new Date(0);

        if (now - lastLogTime >= FOUR_HOURS_MS) {
            // Log success api check
            await successes.insertOne({
                trigger: triggerName,
                function: "heath_api_check",
                api: "Server API",
                status: statusCode,
                timestamp: now
            });

            // Update last success lock time
            await state.updateOne(
                { _id: lockKey },
                { $set: { timestamp: now } },
                { upsert: true }
            );
        }

        return status;

    } catch (error) {

        await errors.insertOne({
            trigger: triggerName,
            function: "heath_api_check",
            api: "Server API",
            error: error.message,
            timestamp: now
        });

        return 500;
    }
};
