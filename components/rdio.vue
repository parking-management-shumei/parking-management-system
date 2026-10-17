<template>
  <div>
    <button v-if="isSupported" @click="startRecognition">开始语音输入</button>
    <button v-if="isSupported" @click="stopRecognition">停止语音输入</button>
    <p v-if="isSupported">识别结果：{{ result }}</p>
    <p v-else>当前浏览器不支持语音识别</p>
  </div>
</template>

<script>
export default {
  name:'VoiceInput',
  data() {
    return {
      recognition: null,
      result: '',
      isSupported: false
    };
  },
  mounted() {
    if ('webkitSpeechRecognition' in window) {
      this.recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
      this.recognition.continuous = true;
      this.recognition.interimResults = true;
      this.recognition.lang = 'zh-CN'; // 设置识别语言
      this.isSupported = true;
    } else {
      console.log('浏览器不支持语音识别');
    }
  },
  methods: {
    startRecognition() {
      if (this.isSupported && this.recognition) {
        this.recognition.start();
        this.recognition.onresult = event => {
          let last = event.results.length - 1;
          let text = event.results[last][0].transcript;
          this.result = text;
        };
        this.recognition.onerror = event => {
          console.error('语音识别错误', event.error);
          switch (event.error) {
            case 'network':
              console.log('网络错误');
              break;
            case 'not-allowed':
              console.log('没有权限');
              break;
            case 'no-speech':
              console.log('没有检测到语音');
              break;
            case 'audio-capture':
              console.log('音频捕获失败');
              break;
            case 'start':
              console.log('语音识别启动失败');
              break;
            default:
              console.log('未知错误');
          }
        };
        this.recognition.onend = () => {
          console.log('语音识别结束');
        };
      } else {
        console.log('语音识别未初始化或浏览器不支持');
      }
    },
    stopRecognition() {
      if (this.isSupported && this.recognition) {
        this.recognition.stop();
      }
    }
  }
};
</script>

<style>
</style>
