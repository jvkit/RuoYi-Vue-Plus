const greet =(name)=>{
    console.log(`hello, ${name}`)
}
const exe3=(fn)=>{
    fn("jvkit");
    fn("kimi");
    fn("deepseek");
}


let time=1
const nums=[1,2,3]
const result = nums.map((n)=>{
    if (time ==1){
        console.log("被判定为一执行一次")
    } else if (time==2){
        console.log("now time =2")
    }
    else console.log("other situation , time here only 3 ,and must exe at the last before [ 3, 5, 7 ]")
    
   
    console.log(`i have exeit ${time} times`);
    time++;
    return  2*n+1;
})
console.log(result)