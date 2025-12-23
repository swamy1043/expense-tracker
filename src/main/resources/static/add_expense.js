const API_BASE = "http://localhost:9090";

// Get username from session (localStorage)
const username = localStorage.getItem("exp_user");

if (!username) {
    window.location.href = "login.html";
}

document.getElementById("expenseForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const category = document.getElementById("category").value.trim();
    const description = document.getElementById("description").value.trim();
    const amount = document.getElementById("amount").value.trim();
    const date = document.getElementById("date").value;

    const data = {
        username: username,
        category: category,
        amount: amount,
        description: description,
        date: date
    };

    try {
        const res = await fetch(`${API_BASE}/expenses/add`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        });

        if (!res.ok) {
            alert("Failed to save expense!");
            return;
        }

        // Success
        alert("Expense added successfully!");
        window.location.href = "dashboard.html";

    } catch (error) {
        console.error("Error adding expense:", error);
        alert("Something went wrong!");
    }
});

