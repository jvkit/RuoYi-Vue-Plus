const base ="http://localhost:8080/test"
const echo = async () => {
    const res = await fetch(`${base}/echo`,{
        method:"POST",
        headers:{"Content-Type":"application/json"},
        body:JSON.stringify({
            "name":"jvkit",
            "age":20
        })
    })
    console.log(await res.json())
}

const play = async () => {
    const res = await fetch(`${base}/play`)
    console.log(await res.json())
}

const main = async () => {
    await echo();
    await play()
}
main().catch(e=>console.error("erro:",e.message));